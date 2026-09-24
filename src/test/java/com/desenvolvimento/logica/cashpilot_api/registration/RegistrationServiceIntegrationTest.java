package com.desenvolvimento.logica.cashpilot_api.registration;

import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipStatus;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.service.RegistrationService;
import com.desenvolvimento.logica.cashpilot_api.subscription.model.SubscriptionStatus;
import com.desenvolvimento.logica.cashpilot_api.subscription.repository.SubscriptionRepository;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.user.entity.PlatformRole;
import com.desenvolvimento.logica.cashpilot_api.user.entity.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class RegistrationServiceIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldRegisterUserOrganizationAndOwnerMembership() {
        String email = "profissional-" + UUID.randomUUID() + "@example.com";
        String password = "Uma senha longa para teste!";

        var request = new RegisterRequest(
                "  Profissional de teste  ",
                "  " + email + "  ",
                password,
                "  Organização de teste  "
        );

        var response = registrationService.register(request);

        entityManager.flush();
        entityManager.clear();

        var user = userRepository.findById(response.userId())
                .orElseThrow();

        var tenant = tenantRepository.findById(response.tenantId())
                .orElseThrow();

        var membership = membershipRepository
                .findByTenant_IdAndUser_Id(
                        response.tenantId(),
                        response.userId()
                )
                .orElseThrow();

        Assertions.assertThat(user.getName()).isEqualTo("Profissional de teste");
        Assertions.assertThat(user.getEmail()).isEqualTo(email);
        Assertions.assertThat(user.getPlatformRole()).isEqualTo(PlatformRole.USER);
        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(user.getEmailVerifiedAt()).isNull();

        Assertions.assertThat(user.getPasswordHash()).isNotEqualTo(password);
        Assertions.assertThat(passwordEncoder.matches(password, user.getPasswordHash()))
                .isTrue();

        Assertions.assertThat(tenant.getName()).isEqualTo("Organização de teste");

        Assertions.assertThat(membership.getRole()).isEqualTo(MembershipRole.OWNER);
        Assertions.assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        Assertions.assertThat(membershipRepository.countByTenant_IdAndStatus(
                tenant.getId(),
                MembershipStatus.ACTIVE
        )).isEqualTo(1L);

        var subscription = subscriptionRepository
                .findByTenant_Id(response.tenantId())
                .orElseThrow();

        Assertions.assertThat(subscription.getStatus())
                .isEqualTo(SubscriptionStatus.TRIALING);

        Assertions.assertThat(subscription.getPlan().getCode()).isEqualTo("TRIAL");
        Assertions.assertThat(subscription.getPlan().getMaxUsers()).isEqualTo(1);
        Assertions.assertThat(subscription.getPlan().getMaxProducts()).isEqualTo(10);
        Assertions.assertThat(subscription.getPlanPrice()).isNull();

        Assertions.assertThat(subscription.getTrialEndsAt())
                .isEqualTo(
                        subscription.getTrialStartsAt().plus(14, ChronoUnit.DAYS)
                );

        Assertions.assertThat(subscription.getCurrentPeriodStartsAt())
                .isEqualTo(subscription.getTrialStartsAt());

        Assertions.assertThat(subscription.getCurrentPeriodEndsAt())
                .isEqualTo(subscription.getTrialEndsAt());

        Assertions.assertThat(subscription.hasAccessAt(subscription.getTrialStartsAt()))
                .isTrue();

        Assertions.assertThat(subscription.hasAccessAt(subscription.getTrialEndsAt()))
                .isFalse();

        Assertions.assertThat(subscription.isCancelAtPeriodEnd()).isFalse();
        Assertions.assertThat(subscription.getCancellationRequestedAt()).isNull();
    }
}
