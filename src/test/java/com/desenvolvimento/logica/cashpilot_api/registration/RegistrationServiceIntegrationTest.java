package com.desenvolvimento.logica.cashpilot_api.registration;

import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipStatus;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.service.RegistrationService;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.trial.repository.TenantTrialRepository;
import com.desenvolvimento.logica.cashpilot_api.user.entity.PlatformRole;
import com.desenvolvimento.logica.cashpilot_api.user.entity.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class RegistrationServiceIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private TenantTrialRepository trialRepository;

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

        var trial = trialRepository.findByTenant_Id(response.tenantId())
                .orElseThrow();

        Assertions.assertThat(trial.getEndsAt())
                .isEqualTo(trial.getStartsAt().plus(14, ChronoUnit.DAYS));

        Assertions.assertThat(trial.isActiveAt(trial.getStartsAt())).isTrue();
        Assertions.assertThat(trial.isActiveAt(trial.getStartsAt().minusSeconds(1))).isFalse();
        Assertions.assertThat(trial.isActiveAt(trial.getEndsAt())).isFalse();
    }
}
