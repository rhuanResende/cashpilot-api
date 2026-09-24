package com.desenvolvimento.logica.cashpilot_api.user.dto;

import com.desenvolvimento.logica.cashpilot_api.shared.dto.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

public record UpdateUserProfileRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter até 150 caracteres.")
        String name,

        @CPF(message = "Informe um CPF válido.")
        String document,

        @Pattern(
                regexp = "\\+[1-9][0-9]{1,14}",
                message = "Informe o telefone com +, código do país e DDD."
        )
        String phone,

        @Valid
        AddressRequest address
) {
    public UpdateUserProfileRequest {
        name = normalize(name);
        document = normalize(document);
        phone = normalize(phone);

        if (document != null) {
            document = document
                    .replace(".", "")
                    .replace("-", "");
        }

        if (phone != null) {
            phone = phone
                    .replace(" ", "")
                    .replace("(", "")
                    .replace(")", "")
                    .replace("-", "");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public String toString() {
        return "UpdateUserProfileRequest[conteúdo omitido]";
    }
}
