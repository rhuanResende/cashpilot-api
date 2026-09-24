package com.desenvolvimento.logica.cashpilot_api.tenant.dto;

import com.desenvolvimento.logica.cashpilot_api.shared.dto.AddressRequest;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.DocumentType;
import com.desenvolvimento.logica.cashpilot_api.tenant.validation.ValidTenantDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@ValidTenantDocument
public record UpdateTenantProfileRequest(
        @NotBlank(message = "O nome da organização é obrigatório.")
        @Size(max = 150, message = "O nome deve ter até 150 caracteres.")
        String name,

        @Size(max = 200, message = "O nome legal deve ter até 200 caracteres.")
        String legalName,

        DocumentType documentType,

        @Size(max = 14, message = "O documento deve ter até 14 caracteres.")
        String document,

        @Email(message = "Informe um e-mail de contato válido.")
        @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
        String contactEmail,

        @Pattern(
                regexp = "\\+[1-9][0-9]{1,14}",
                message = "Informe o telefone com +, código do país e DDD."
        )
        String phone,

        @Valid
        AddressRequest address
) {
    public UpdateTenantProfileRequest {
        name = normalize(name);
        legalName = normalize(legalName);
        document = normalize(document);
        contactEmail = normalize(contactEmail);
        phone = normalize(phone);

        if (document != null) {
            document = document
                    .replace(".", "")
                    .replace("/", "")
                    .replace("-", "")
                    .toUpperCase(Locale.ROOT);
        }

        if (contactEmail != null) {
            contactEmail = contactEmail.toLowerCase(Locale.ROOT);
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
        return "UpdateTenantProfileRequest[conteúdo omitido]";
    }
}
