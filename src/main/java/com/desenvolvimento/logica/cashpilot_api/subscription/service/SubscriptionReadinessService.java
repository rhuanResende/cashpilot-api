package com.desenvolvimento.logica.cashpilot_api.subscription.service;

import com.desenvolvimento.logica.cashpilot_api.shared.mapper.AddressMapper;
import com.desenvolvimento.logica.cashpilot_api.shared.model.Address;
import com.desenvolvimento.logica.cashpilot_api.subscription.dto.ProfileRequirement;
import com.desenvolvimento.logica.cashpilot_api.subscription.dto.SubscriptionReadinessResponse;
import com.desenvolvimento.logica.cashpilot_api.tenant.dto.UpdateTenantProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import com.desenvolvimento.logica.cashpilot_api.user.dto.UpdateUserProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SubscriptionReadinessService {

    private final Validator validator;

    public SubscriptionReadinessService(Validator validator) {
        this.validator = validator;
    }

    public SubscriptionReadinessResponse check(User owner, Tenant tenant) {
        Objects.requireNonNull(owner, "O responsável é obrigatório.");
        Objects.requireNonNull(tenant, "A organização é obrigatória.");

        List<ProfileRequirement> pending = new ArrayList<>();

        requireText(pending, "USER", "name", owner.getName());
        requireText(pending, "USER", "email", owner.getEmail());
        requireText(pending, "USER", "document", owner.getDocument());
        requireText(pending, "USER", "phone", owner.getPhone());

        if (owner.getEmailVerifiedAt() == null) {
            pending.add(new ProfileRequirement(
                    "USER",
                    "emailVerified",
                    "Confirme o e-mail do responsável."
            ));
        }

        checkAddress(pending, "USER", owner.getAddress());

        requireText(pending, "TENANT", "name", tenant.getName());
        requireText(pending, "TENANT", "legalName", tenant.getLegalName());
        requireText(pending, "TENANT", "document", tenant.getDocument());
        requireText(pending, "TENANT", "contactEmail", tenant.getContactEmail());
        requireText(pending, "TENANT", "phone", tenant.getPhone());

        if (tenant.getDocumentType() == null) {
            pending.add(new ProfileRequirement(
                    "TENANT",
                    "documentType",
                    "Informe o tipo do documento da empresa."
            ));
        }

        checkAddress(pending, "TENANT", tenant.getAddress());

        addValidationErrors(
                pending,
                "USER",
                new UpdateUserProfileRequest(
                        owner.getName(),
                        owner.getDocument(),
                        owner.getPhone(),
                        AddressMapper.toRequest(owner.getAddress())
                )
        );

        addValidationErrors(
                pending,
                "USER",
                new EmailCheck(owner.getEmail())
        );

        addValidationErrors(
                pending,
                "TENANT",
                new UpdateTenantProfileRequest(
                        tenant.getName(),
                        tenant.getLegalName(),
                        tenant.getDocumentType(),
                        tenant.getDocument(),
                        tenant.getContactEmail(),
                        tenant.getPhone(),
                        AddressMapper.toRequest(tenant.getAddress())
                )
        );

        return new SubscriptionReadinessResponse(
                pending.isEmpty(),
                pending
        );
    }

    private void checkAddress(
            List<ProfileRequirement> pending,
            String scope,
            Address address) {

        if (address == null) {
            pending.add(new ProfileRequirement(
                    scope,
                    "address",
                    "Preencha o endereço completo."
            ));
            return;
        }

        requireText(pending, scope, "address.postalCode", address.getPostalCode());
        requireText(pending, scope, "address.street", address.getStreet());
        requireText(pending, scope, "address.number", address.getNumber());
        requireText(pending, scope, "address.neighborhood", address.getNeighborhood());
        requireText(pending, scope, "address.city", address.getCity());
        requireText(pending, scope, "address.state", address.getState());
        requireText(pending, scope, "address.countryCode", address.getCountryCode());
    }

    private void requireText(
            List<ProfileRequirement> pending,
            String scope,
            String field,
            String value) {

        if (value == null || value.isBlank()) {
            pending.add(new ProfileRequirement(
                    scope,
                    field,
                    "Preenchimento obrigatório para contratar."
            ));
        }
    }

    private <T> void addValidationErrors(
            List<ProfileRequirement> pending,
            String scope,
            T value) {

        for (var violation : validator.validate(value)) {
            String field = violation.getPropertyPath().toString();

            boolean alreadyListed = pending.stream()
                    .anyMatch(item ->
                            item.scope().equals(scope)
                                    && item.field().equals(field)
                    );

            if (!alreadyListed) {
                pending.add(new ProfileRequirement(
                        scope,
                        field,
                        violation.getMessage()
                ));
            }
        }
    }

    public record EmailCheck(
            @NotBlank(message = "O e-mail é obrigatório.")
            @Email(message = "Informe um e-mail válido.")
            @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
            String email
    ) {
    }
}
