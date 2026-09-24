package com.desenvolvimento.logica.cashpilot_api.verification.event;

import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;

import java.util.Objects;

public record EmailVerificationRequestedEvent(
        IssuedEmailVerification verification
) {
    public EmailVerificationRequestedEvent {
        Objects.requireNonNull(
                verification,
                "Os dados de confirmação são obrigatórios"
        );
    }

    @Override
    public String toString() {
        return "EmailVerificationRequestedEvent[verification=REDACTED]";
    }
}
