package com.desenvolvimento.logica.cashpilot_api.auth.dto;

import java.util.Objects;
import java.util.UUID;

public record AuthenticationRequestContext(
        UUID requestId,
        String sourceIp,
        String userAgent
) {
    public AuthenticationRequestContext {
        Objects.requireNonNull(
                requestId,
                "O identificador da requisição é obrigatório"
        );
    }
}
