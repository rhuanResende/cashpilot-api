package com.desenvolvimento.logica.cashpilot_api.shared.exception;

import java.time.Duration;
import java.util.Objects;

public class TooManyLoginAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Muitas tentativas. Aguarde antes de tentar novamente.");

        Objects.requireNonNull(retryAfter, "O prazo é obrigatório");

        if (retryAfter.isNegative() || retryAfter.isZero()) {
            throw new IllegalArgumentException(
                    "O prazo deve ser positivo"
            );
        }

        this.retryAfterSeconds =
                Math.max(1L, (retryAfter.toMillis() + 999L) / 1000L);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
