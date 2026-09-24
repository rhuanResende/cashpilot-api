package com.desenvolvimento.logica.cashpilot_api.tenant.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TenantDocumentValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTenantDocument {

    String message() default "Documento inválido para o tipo informado.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
