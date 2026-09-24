package com.desenvolvimento.logica.cashpilot_api.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmEmailRequest(
        @NotBlank(message = "O token é obrigatório")
        @Pattern(
                regexp = "[A-Za-z0-9_-]{43}",
                message = "O token possui formato inválido"
        )
        String token
) {
    @Override
    public String toString() {
        return "ConfirmEmailRequest[token=REDACTED]";
    }
}
