package com.desenvolvimento.logica.cashpilot_api.auth;

import com.desenvolvimento.logica.cashpilot_api.auth.config.LoginRateLimitProperties;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.LoginRateLimitResult;
import com.desenvolvimento.logica.cashpilot_api.auth.service.LoginIpRateLimiter;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.LoginRateLimitUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(LoginIpRateLimiterIntegrationTest.Config.class)
class LoginIpRateLimiterIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private InetAddress address;
    private String prefix;
    private String key;

    @BeforeEach
    void setUp() throws Exception {
        address = InetAddress.getByAddress(
                new byte[]{(byte) 192, 0, 2, 10}
        );

        prefix = "cashpilot:test:rate-limit:" + UUID.randomUUID();

        key = prefix + ":" + HexFormat.of()
                .formatHex(address.getAddress());
    }

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(key);
    }

    @Test
    void shouldAllowUpToLimitAndRejectFurtherAttempts() {
        var limiter = limiter(3, Duration.ofMinutes(1));

        for (int i = 1; i <= 3; i++) {
            var result = limiter.consume(address);

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(3 - i);
            assertThat(result.retryAfter()).isEqualTo(Duration.ZERO);
        }

        var denied = limiter.consume(address);

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.remaining()).isZero();
        assertThat(denied.retryAfter().toMillis())
                .isBetween(1L, 60_000L);

        // A tentativa recusada não incrementa o contador.
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("3");
    }

    @Test
    void shouldNotExtendExpirationWhenLimitIsReached() {
        var limiter = limiter(1, Duration.ofMinutes(1));

        limiter.consume(address);

        // Encurta o prazo para detectar uma renovação indevida para 1 minuto.
        assertThat(
                redisTemplate.expire(key, Duration.ofSeconds(10))
        ).isTrue();

        var denied = limiter.consume(address);

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfter().toMillis())
                .isBetween(1L, 10_000L);

        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS))
                .isBetween(1L, 10_000L);
    }

    @Test
    void shouldAllowAgainAfterKeyExpires() {
        var limiter = limiter(1, Duration.ofSeconds(1));

        assertThat(limiter.consume(address).allowed()).isTrue();

        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS))
                .isBetween(1L, 1_000L);

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(redisTemplate.hasKey(key)).isFalse()
                );

        var result = limiter.consume(address);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isZero();
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("1");
    }

    @Test
    void shouldRespectLimitWithConcurrentRequests() throws Exception {
        var limiter = limiter(3, Duration.ofMinutes(1));

        int requests = 10;

        var executor = Executors.newFixedThreadPool(requests);
        var ready = new CountDownLatch(requests);
        var start = new CountDownLatch(1);

        List<Future<LoginRateLimitResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Tempo esgotado aguardando início."
                        );
                    }

                    return limiter.consume(address);
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<LoginRateLimitResult> results = new ArrayList<>();

            for (var future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(results.stream()
                    .filter(LoginRateLimitResult::allowed)
                    .count())
                    .isEqualTo(3);

            assertThat(results.stream()
                    .filter(result -> !result.allowed())
                    .count())
                    .isEqualTo(7);

            assertThat(redisTemplate.opsForValue().get(key))
                    .isEqualTo("3");
        } finally {
            start.countDown();
            executor.shutdownNow();

            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .as("As tarefas devem terminar antes da limpeza")
                    .isTrue();
        }
    }

    @Test
    void shouldRejectCounterWithoutExpiration() {
        var limiter = limiter(3, Duration.ofMinutes(1));

        // Simula uma chave inconsistente, sem prazo de expiração.
        redisTemplate.opsForValue().set(key, "1");

        assertThatThrownBy(() -> limiter.consume(address))
                .isInstanceOf(LoginRateLimitUnavailableException.class);

        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("1");
        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS))
                .isEqualTo(-1L);
    }

    @Test
    void shouldRejectInvalidCounterValue() {
        var limiter = limiter(3, Duration.ofMinutes(1));

        redisTemplate.opsForValue().set(
                key,
                "valor-invalido",
                Duration.ofMinutes(1)
        );

        assertThatThrownBy(() -> limiter.consume(address))
                .isInstanceOf(LoginRateLimitUnavailableException.class);

        assertThat(redisTemplate.opsForValue().get(key))
                .isEqualTo("valor-invalido");
    }

    private LoginIpRateLimiter limiter(int limit, Duration window) {
        return new LoginIpRateLimiter(
                redisTemplate,
                new LoginRateLimitProperties(limit, window, prefix)
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean
        LettuceConnectionFactory redisConnectionFactory() {
            var server = new RedisStandaloneConfiguration(
                    "127.0.0.1",
                    6379
            );
            server.setDatabase(1);

            var client = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(2))
                    .build();

            return new LettuceConnectionFactory(server, client);
        }

        @Bean
        StringRedisTemplate redisTemplate(
                LettuceConnectionFactory connectionFactory
        ) {
            return new StringRedisTemplate(connectionFactory);
        }
    }
}