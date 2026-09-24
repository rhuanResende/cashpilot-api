package com.desenvolvimento.logica.cashpilot_api.subscription;

import com.desenvolvimento.logica.cashpilot_api.shared.model.Address;
import com.desenvolvimento.logica.cashpilot_api.subscription.dto.ProfileRequirement;
import com.desenvolvimento.logica.cashpilot_api.subscription.service.SubscriptionReadinessService;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.DocumentType;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(SubscriptionReadinessServiceTest.Config.class)
public class SubscriptionReadinessServiceTest {

    @Autowired
    private SubscriptionReadinessService service;

    @Configuration(proxyBeanMethods = false)
    @Import(SubscriptionReadinessService.class)
    static class Config {

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Test
    void shouldListMissingFieldsFromInitialRegistration() {
        User owner = newUser();
        Tenant tenant = new Tenant("Empresa de teste");

        var result = service.check(owner, tenant);

        assertThat(result.ready()).isFalse();

        assertThat(result.pendingFields())
                .extracting(this::fieldKey)
                .containsExactlyInAnyOrder(
                        "USER.document",
                        "USER.phone",
                        "USER.emailVerified",
                        "USER.address",
                        "TENANT.legalName",
                        "TENANT.document",
                        "TENANT.contactEmail",
                        "TENANT.phone",
                        "TENANT.documentType",
                        "TENANT.address"
                );
    }

    @Test
    void shouldAcceptCompleteProfilesWithVerifiedEmail() {
        User owner = completeOwner();
        owner.markEmailVerified();

        var result = service.check(owner, completeTenant());

        assertThat(result.ready()).isTrue();
        assertThat(result.pendingFields()).isEmpty();
    }

    @Test
    void shouldRequireEmailVerificationEvenWithCompleteProfiles() {
        var result = service.check(completeOwner(), completeTenant());

        assertThat(result.ready()).isFalse();

        assertThat(result.pendingFields())
                .extracting(this::fieldKey)
                .containsExactly("USER.emailVerified");
    }

    @Test
    void shouldIdentifyMissingFieldInsideExistingAddress() {
        User owner = completeOwner();
        owner.markEmailVerified();

        Tenant tenant = completeTenant();

        tenant.updateProfile(
                tenant.getName(),
                tenant.getLegalName(),
                tenant.getDocumentType(),
                tenant.getDocument(),
                tenant.getContactEmail(),
                tenant.getPhone(),
                new Address(
                        "01310100",
                        "Rua de teste",
                        "S/N",
                        null,
                        "Bairro de teste",
                        "   ",
                        "SP",
                        "BR"
                )
        );

        var result = service.check(owner, tenant);

        assertThat(result.ready()).isFalse();

        assertThat(result.pendingFields())
                .extracting(this::fieldKey)
                .containsExactly("TENANT.address.city");
    }

    @Test
    void shouldRejectInvalidCpfEvenWhenProfilesAreComplete() {
        User owner = completeOwner();
        owner.markEmailVerified();

        owner.updateProfile(
                owner.getName(),
                "11111111111",
                owner.getPhone(),
                owner.getAddress()
        );

        var result = service.check(owner, completeTenant());

        assertThat(result.ready()).isFalse();

        assertThat(result.pendingFields())
                .extracting(this::fieldKey)
                .containsExactly("USER.document");
    }

    private User newUser() {
        // Valor fictício: este teste não autentica nem persiste usuários.
        return new User(
                "Profissional de teste",
                "profissional@example.com",
                "hash-ficticio-apenas-para-teste"
        );
    }

    private User completeOwner() {
        User owner = newUser();

        owner.updateProfile(
                owner.getName(),
                "52998224725",
                "+5511999999999",
                completeAddress()
        );

        return owner;
    }

    private Tenant completeTenant() {
        Tenant tenant = new Tenant("Empresa de teste");

        tenant.updateProfile(
                tenant.getName(),
                "Empresa de Teste Ltda.",
                DocumentType.CNPJ,
                "11222333000181",
                "contato@example.com",
                "+5511999999999",
                completeAddress()
        );

        return tenant;
    }

    private Address completeAddress() {
        return new Address(
                "01310100",
                "Rua de teste",
                "S/N",
                null,
                "Bairro de teste",
                "São Paulo",
                "SP",
                "BR"
        );
    }

    private String fieldKey(ProfileRequirement requirement) {
        return requirement.scope() + "." + requirement.field();
    }
}
