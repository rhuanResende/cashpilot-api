package com.desenvolvimento.logica.cashpilot_api.auth;

import com.desenvolvimento.logica.cashpilot_api.auth.config.LoginRateLimitProperties;
import com.desenvolvimento.logica.cashpilot_api.auth.service.LoginIpRateLimiter;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.LoginRateLimitUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LoginIpRateLimiterTest {

    @Test
    void shouldRejectAttemptWhenRedisIsUnavailable() throws Exception {
        var connectionFailure = new RedisConnectionFailureException(
                "Falha de conexão simulada"
        );

        StringRedisTemplate redisTemplate = mock(
                StringRedisTemplate.class,
                invocation -> {
                    throw connectionFailure;
                }
        );

        var limiter = new LoginIpRateLimiter(
                redisTemplate,
                new LoginRateLimitProperties(
                        20,
                        Duration.ofMinutes(1),
                        "cashpilot:test:unavailable"
                )
        );

        InetAddress address = InetAddress.getByAddress(
                new byte[]{(byte) 192, 0, 2, 10}
        );

        assertThatThrownBy(() -> limiter.consume(address))
                .isInstanceOf(LoginRateLimitUnavailableException.class)
                .hasCause(connectionFailure);
    }
}