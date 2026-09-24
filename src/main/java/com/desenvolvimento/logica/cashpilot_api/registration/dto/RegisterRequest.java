package com.desenvolvimento.logica.cashpilot_api.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter até 150 caracteres.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(
                min = 12,
                max = 128,
                message = "A senha deve ter entre 12 e 128 caracteres."
        )
        String password,

        @NotBlank(message = "O nome da organização é obrigatório.")
        @Size(
                max = 150,
                message = "O nome da organização deve ter até 150 caracteres."
        )
        String organizationName
) {

    public RegisterRequest {
        name = name == null ? null : name.strip();

        email = email == null
                ? null
                : email.strip().toLowerCase(Locale.ROOT);

        organizationName = organizationName == null
                ? null
                : organizationName.strip();
    }

    @Override
    public String toString() {
        return "RegisterRequest[conteúdo omitido]";
    }
}
