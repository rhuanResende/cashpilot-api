package com.desenvolvimento.logica.cashpilot_api.subscription;

import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.model.TenantMembership;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.plan.repository.PlanRepository;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.ForbiddenOperationException;
import com.desenvolvimento.logica.cashpilot_api.subscription.model.Subscription;
import com.desenvolvimento.logica.cashpilot_api.subscription.repository.SubscriptionRepository;
import com.desenvolvimento.logica.cashpilot_api.subscription.service.SubscriptionCheckoutService;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class SubscriptionCheckoutServiceIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private SubscriptionCheckoutService checkoutService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldAllowOwnerToCheckPendingFields() {
        Fixture fixture = createFixture(MembershipRole.OWNER);

        var result = checkoutService.checkReadiness(
                fixture.userId(),
                fixture.tenantId()
        );

        assertThat(result.ready()).isFalse();

        assertThat(result.pendingFields())
                .extracting(item -> item.scope() + "." + item.field())
                .contains(
                        "USER.document",
                        "USER.emailVerified",
                        "TENANT.document",
                        "TENANT.address"
                );
    }

    @ParameterizedTest
    @EnumSource(
            value = MembershipRole.class,
            names = {"ADMIN", "OPERATOR"}
    )
    void shouldRejectMembersWhoAreNotOwners(MembershipRole role) {
        Fixture fixture = createFixture(role);

        assertThatThrownBy(() -> checkoutService.checkReadiness(
                fixture.userId(),
                fixture.tenantId()
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void shouldRejectOwnerOfAnotherTenant() {
        Fixture first = createFixture(MembershipRole.OWNER);
        Fixture second = createFixture(MembershipRole.OWNER);

        assertThatThrownBy(() -> checkoutService.checkReadiness(
                first.userId(),
                second.tenantId()
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void shouldAllowRegularizationAfterTrialExpiration() {
        Fixture fixture = createFixture(MembershipRole.OWNER);

        var tenant = tenantRepository.findById(fixture.tenantId())
                .orElseThrow();

        var plan = planRepository.findByCode("TRIAL").orElseThrow();
        Instant now = Instant.now();

        var subscription = Subscription.startTrial(
                tenant,
                plan,
                now.minus(
                        plan.getTrialDurationDays() + 1L,
                        ChronoUnit.DAYS
                )
        );

        subscriptionRepository.saveAndFlush(subscription);
        entityManager.clear();

        var persisted = subscriptionRepository
                .findByTenant_Id(fixture.tenantId())
                .orElseThrow();

        assertThat(persisted.hasAccessAt(now)).isFalse();

        var result = checkoutService.checkReadiness(
                fixture.userId(),
                fixture.tenantId()
        );

        // O responsável pode consultar o que falta para regularizar.
        assertThat(result.ready()).isFalse();
        assertThat(result.pendingFields()).isNotEmpty();
    }

    private Fixture createFixture(MembershipRole role) {
        User user = userRepository.save(new User(
                "Profissional de teste",
                "checkout-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Uma senha longa para teste!")
        ));

        Tenant tenant = tenantRepository.save(
                new Tenant("Empresa de teste")
        );

        membershipRepository.save(
                new TenantMembership(tenant, user, role)
        );

        entityManager.flush();

        Fixture fixture = new Fixture(user.getId(), tenant.getId());
        entityManager.clear();

        return fixture;
    }

    private record Fixture(UUID userId, UUID tenantId) {
    }
}
