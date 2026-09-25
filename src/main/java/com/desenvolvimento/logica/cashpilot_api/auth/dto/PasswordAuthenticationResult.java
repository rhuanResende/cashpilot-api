package com.desenvolvimento.logica.cashpilot_api.auth.dto;

import com.desenvolvimento.logica.cashpilot_api.auth.model.PasswordAuthenticationStatus;

import java.util.Objects;
import java.util.UUID;

public record PasswordAuthenticationResult(
        PasswordAuthenticationStatus status,
        UUID userId
) {
    public PasswordAuthenticationResult {
        Objects.requireNonNull(status, "O resultado é obrigatório");

        if (status == PasswordAuthenticationStatus.PASSWORD_VERIFIED
                && userId == null) {
            throw new IllegalArgumentException(
                    "O usuário é obrigatório quando a senha foi validada"
            );
        }

        if (status == PasswordAuthenticationStatus.DENIED
                && userId != null) {
            throw new IllegalArgumentException(
                    "O resultado negado não deve identificar o usuário"
            );
        }
    }

    public static PasswordAuthenticationResult verified(UUID userId) {
        return new PasswordAuthenticationResult(
                PasswordAuthenticationStatus.PASSWORD_VERIFIED,
                userId
        );
    }

    public static PasswordAuthenticationResult denied() {
        return new PasswordAuthenticationResult(
                PasswordAuthenticationStatus.DENIED,
                null
        );
    }
}
