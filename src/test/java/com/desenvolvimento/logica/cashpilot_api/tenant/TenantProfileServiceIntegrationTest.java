package com.desenvolvimento.logica.cashpilot_api.tenant;

import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.entity.TenantMembership;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.ForbiddenOperationException;
import com.desenvolvimento.logica.cashpilot_api.tenant.dto.UpdateTenantProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.DocumentType;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.TenantStatus;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.tenant.service.TenantProfileService;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class TenantProfileServiceIntegrationTest {

    @Autowired
    private TenantProfileService profileService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(
            value = MembershipRole.class,
            names = {"OWNER", "ADMIN"}
    )
    void shouldAllowOwnerAndAdminToUpdateProfile(MembershipRole role) {
        Fixture fixture = createFixture(role);

        profileService.updateProfile(
                fixture.userId(),
                fixture.tenantId(),
                validRequest()
        );

        entityManager.flush();
        entityManager.clear();

        Tenant updated = tenantRepository.findById(fixture.tenantId())
                .orElseThrow();

        assertThat(updated.getName()).isEqualTo("Empresa atualizada");
        assertThat(updated.getLegalName()).isEqualTo("Empresa de Teste Ltda.");
        assertThat(updated.getDocumentType()).isEqualTo(DocumentType.CNPJ);
        assertThat(updated.getDocument()).isEqualTo("11222333000181");
        assertThat(updated.getContactEmail()).isEqualTo("contato@example.com");
        assertThat(updated.getPhone()).isEqualTo("+5511999999999");
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void shouldRejectOperator() {
        Fixture fixture = createFixture(MembershipRole.OPERATOR);

        assertThatThrownBy(() -> profileService.updateProfile(
                fixture.userId(),
                fixture.tenantId(),
                validRequest()
        )).isInstanceOf(ForbiddenOperationException.class);

        assertProfileUnchanged(fixture.tenantId());
    }

    @Test
    void shouldRejectOwnerOfAnotherTenant() {
        Fixture first = createFixture(MembershipRole.OWNER);
        Fixture second = createFixture(MembershipRole.OWNER);

        assertThatThrownBy(() -> profileService.updateProfile(
                first.userId(),
                second.tenantId(),
                validRequest()
        )).isInstanceOf(ForbiddenOperationException.class);

        assertProfileUnchanged(first.tenantId());
        assertProfileUnchanged(second.tenantId());
    }

    private Fixture createFixture(MembershipRole role) {
        User user = userRepository.save(new User(
                "Profissional de teste",
                "empresa-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Uma senha longa para teste!")
        ));

        Tenant tenant = tenantRepository.save(
                new Tenant("Empresa original")
        );

        membershipRepository.save(
                new TenantMembership(tenant, user, role)
        );

        entityManager.flush();

        return new Fixture(user.getId(), tenant.getId());
    }

    private UpdateTenantProfileRequest validRequest() {
        return new UpdateTenantProfileRequest(
                "  Empresa atualizada  ",
                "Empresa de Teste Ltda.",
                DocumentType.CNPJ,
                "11.222.333/0001-81",
                "CONTATO@example.com",
                "+55 (11) 99999-9999",
                null
        );
    }

    private void assertProfileUnchanged(UUID tenantId) {
        entityManager.flush();
        entityManager.clear();

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

        assertThat(tenant.getName()).isEqualTo("Empresa original");
        assertThat(tenant.getLegalName()).isNull();
        assertThat(tenant.getDocumentType()).isNull();
        assertThat(tenant.getDocument()).isNull();
        assertThat(tenant.getContactEmail()).isNull();
        assertThat(tenant.getPhone()).isNull();
    }

    private record Fixture(UUID userId, UUID tenantId) {
    }
}
