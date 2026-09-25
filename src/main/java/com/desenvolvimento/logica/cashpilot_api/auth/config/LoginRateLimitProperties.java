package com.desenvolvimento.logica.cashpilot_api.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(
        prefix = "app.security.authentication.ip-rate-limit"
)
public record LoginRateLimitProperties(
        @Min(1)
        int maxRequests,

        @NotNull
        Duration window,

        @NotBlank
        String keyPrefix
) {
    public LoginRateLimitProperties {
        if (window != null
                && (window.compareTo(Duration.ofSeconds(1)) < 0
                || window.compareTo(Duration.ofDays(1)) > 0)) {
            throw new IllegalArgumentException(
                    "A janela deve estar entre 1 segundo e 1 dia"
            );
        }

        if (keyPrefix != null) {
            keyPrefix = keyPrefix.strip();
        }
    }
}
