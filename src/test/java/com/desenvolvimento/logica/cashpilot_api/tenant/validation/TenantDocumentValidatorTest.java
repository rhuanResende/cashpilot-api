package com.desenvolvimento.logica.cashpilot_api.tenant.validation;


import com.desenvolvimento.logica.cashpilot_api.tenant.dto.UpdateTenantProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.DocumentType;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(TenantDocumentValidatorTest.Config.class)
public class TenantDocumentValidatorTest {

    @Autowired
    private Validator validator;

    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Test
    void shouldAcceptMissingDocumentDuringTrial() {
        var request = request(null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "CPF, 529.982.247-25, 52998224725",
            "CNPJ, 11.222.333/0001-81, 11222333000181",
            "CNPJ, 12.abc.345/01de-35, 12ABC34501DE35"
    })
    void shouldNormalizeAndAcceptValidDocuments(
            DocumentType type,
            String input,
            String expected) {

        var request = request(type, input);

        assertThat(request.document()).isEqualTo(expected);
        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "CPF, 52998224724",
            "CPF, 11111111111",
            "CNPJ, 11222333000180",
            "CNPJ, 12ABC34501DE34",
            "CPF, 11222333000181",
            "CNPJ, 52998224725"
    })
    void shouldRejectInvalidOrMismatchedDocuments(
            DocumentType type,
            String document) {

        var violations = validator.validate(request(type, document));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("document");
    }

    @Test
    void shouldRejectDocumentWithoutType() {
        var violations = validator.validate(request(null, "52998224725"));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("documentType");
    }

    @Test
    void shouldRejectTypeWithoutDocument() {
        var violations = validator.validate(request(DocumentType.CPF, null));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("document");
    }

    private UpdateTenantProfileRequest request(
            DocumentType type,
            String document) {

        return new UpdateTenantProfileRequest(
                "Empresa de teste",
                null,
                type,
                document,
                null,
                null,
                null
        );
    }
}
