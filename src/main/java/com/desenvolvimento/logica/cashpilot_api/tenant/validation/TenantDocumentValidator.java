package com.desenvolvimento.logica.cashpilot_api.tenant.validation;

import com.desenvolvimento.logica.cashpilot_api.tenant.dto.UpdateTenantProfileRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Validator;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

public class TenantDocumentValidator implements
        ConstraintValidator<ValidTenantDocument, UpdateTenantProfileRequest> {

    private final Validator validator;

    public TenantDocumentValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean isValid(
            UpdateTenantProfileRequest request,
            ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        if (request.documentType() == null && request.document() == null) {
            return true;
        }

        if (request.documentType() == null || request.document() == null) {
            return reject(
                    context,
                    request.documentType() == null ? "documentType" : "document",
                    "Informe o documento e seu tipo juntos."
            );
        }

        boolean valid = switch (request.documentType()) {
            case CPF -> validator.validate(
                    new CpfValue(request.document())
            ).isEmpty();

            case CNPJ -> validator.validate(
                    new CnpjValue(request.document())
            ).isEmpty();
        };

        if (!valid) {
            return reject(
                    context,
                    "document",
                    context.getDefaultConstraintMessageTemplate()
            );
        }

        return true;
    }

    private boolean reject(
            ConstraintValidatorContext context,
            String field,
            String message) {

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();

        return false;
    }

    public record CpfValue(@CPF String value) {
    }

    public record CnpjValue(
            @CNPJ(format = CNPJ.Format.ALPHANUMERIC) String value
    ) {
    }
}
