package com.desenvolvimento.logica.cashpilot_api.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.authentication")
public record AuthenticationPolicyProperties(
        @NotNull @Valid
        AttemptPolicy password,

        @NotNull @Valid
        AttemptPolicy mfa
) {
    public record AttemptPolicy(

            @Min(1)
            int maxFailedAttempts,

            @NotNull
            Duration failureWindow,

            @NotNull
            Duration lockDuration

    ) {

        public AttemptPolicy {
            requirePositive(failureWindow, "failureWindow");
            requirePositive(lockDuration, "lockDuration");
        }

        private static void requirePositive(
                Duration value,
                String field
        ) {
            if (value != null && (value.isZero() || value.isNegative())) {
                throw new IllegalArgumentException(
                        field + " deve ser uma duração positiva"
                );
            }
        }
    }
}
