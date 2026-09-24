package com.desenvolvimento.logica.cashpilot_api.verification.dto;

import java.time.Instant;

public record IssuedEmailVerification(
        String email,
        String token,
        Instant expiresAt
) {
    @Override
    public String toString() {
        return "IssuedEmailVerification[conteúdo omitido]";
    }
}
