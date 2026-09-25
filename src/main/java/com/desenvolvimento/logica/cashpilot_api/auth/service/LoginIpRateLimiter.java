package com.desenvolvimento.logica.cashpilot_api.auth.service;

import com.desenvolvimento.logica.cashpilot_api.auth.config.LoginRateLimitProperties;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.LoginRateLimitResult;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.LoginRateLimitUnavailableException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class LoginIpRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitProperties properties;

    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> script;

    public LoginIpRateLimiter(
            StringRedisTemplate redisTemplate,
            LoginRateLimitProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        this.script = new DefaultRedisScript<>();
        this.script.setLocation(
                new ClassPathResource("redis/login-rate-limit.lua")
        );
        this.script.setResultType(List.class);

        // Detecta arquivo ausente já na inicialização.
        this.script.getScriptAsString();
    }

    public LoginRateLimitResult consume(InetAddress clientAddress) {
        Objects.requireNonNull(
                clientAddress,
                "O endereço do cliente é obrigatório"
        );

        String addressKey = HexFormat.of()
                .formatHex(clientAddress.getAddress());

        String key = properties.keyPrefix() + ":" + addressKey;

        List<?> response;

        try {
            response = redisTemplate.execute(
                    script,
                    List.of(key),
                    Integer.toString(properties.maxRequests()),
                    Long.toString(properties.window().toMillis())
            );
        } catch (DataAccessException exception) {
            throw new LoginRateLimitUnavailableException(exception);
        }

        return decode(response);
    }

    private LoginRateLimitResult decode(List<?> response) {
        if (response == null || response.size() != 3) {
            throw new LoginRateLimitUnavailableException();
        }

        if (!(response.get(0) instanceof Long allowed)
                || !(response.get(1) instanceof Long remaining)
                || !(response.get(2) instanceof Long retryMillis)) {
            throw new LoginRateLimitUnavailableException();
        }

        if (allowed != 0L && allowed != 1L) {
            throw new LoginRateLimitUnavailableException();
        }

        if (remaining < 0
                || remaining >= properties.maxRequests()
                || retryMillis < 0
                || retryMillis > properties.window().toMillis()) {
            throw new LoginRateLimitUnavailableException();
        }

        if (allowed == 1L && retryMillis != 0L) {
            throw new LoginRateLimitUnavailableException();
        }

        if (allowed == 0L && (remaining != 0L || retryMillis == 0L)) {
            throw new LoginRateLimitUnavailableException();
        }

        return new LoginRateLimitResult(
                allowed == 1L,
                remaining.intValue(),
                Duration.ofMillis(retryMillis)
        );
    }
}
