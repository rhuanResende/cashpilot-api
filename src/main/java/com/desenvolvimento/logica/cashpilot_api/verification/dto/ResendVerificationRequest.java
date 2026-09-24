package com.desenvolvimento.logica.cashpilot_api.verification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record ResendVerificationRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter até 254 caracteres")
        String email
) {
    public ResendVerificationRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public String toString() {
        return "ResendVerificationRequest[email=REDACTED]";
    }
}
