package com.desenvolvimento.logica.cashpilot_api.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record AddressRequest(
        @NotBlank(message = "O CEP é obrigatório.")
        @Pattern(
                regexp = "[0-9]{8}",
                message = "O CEP deve conter 8 dígitos."
        )
        String postalCode,

        @NotBlank(message = "O logradouro é obrigatório.")
        @Size(max = 200, message = "O logradouro deve ter até 200 caracteres.")
        String street,

        @NotBlank(message = "O número é obrigatório. Use S/N quando não houver.")
        @Size(max = 20, message = "O número deve ter até 20 caracteres.")
        String number,

        @Size(max = 150, message = "O complemento deve ter até 150 caracteres.")
        String complement,

        @NotBlank(message = "O bairro é obrigatório.")
        @Size(max = 100, message = "O bairro deve ter até 100 caracteres.")
        String neighborhood,

        @NotBlank(message = "A cidade é obrigatória.")
        @Size(max = 100, message = "A cidade deve ter até 100 caracteres.")
        String city,

        @NotBlank(message = "A UF é obrigatória.")
        @Pattern(
                regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "Informe uma UF brasileira válida."
        )
        String state,

        @NotBlank(message = "O país é obrigatório.")
        @Pattern(
                regexp = "BR",
                message = "Neste momento, apenas endereços brasileiros são aceitos."
        )
        String countryCode
) {
    public AddressRequest {
        postalCode = normalize(postalCode);

        if (postalCode != null) {
            postalCode = postalCode.replace("-", "");
        }

        street = normalize(street);
        number = normalize(number);
        complement = normalize(complement);
        neighborhood = normalize(neighborhood);
        city = normalize(city);
        state = uppercase(state);
        countryCode = uppercase(countryCode);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String uppercase(String value) {
        String normalized = normalize(value);
        return normalized == null
                ? null
                : normalized.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "AddressRequest[conteúdo omitido]";
    }
}
