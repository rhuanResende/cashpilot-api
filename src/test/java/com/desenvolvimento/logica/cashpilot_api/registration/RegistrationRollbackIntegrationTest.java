package com.desenvolvimento.logica.cashpilot_api.registration;

import com.desenvolvimento.logica.cashpilot_api.membership.entity.TenantMembership;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.service.RegistrationService;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class RegistrationRollbackIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private TenantMembershipRepository membershipRepository;

    @Test
    void shouldRollbackRegistrationWhenMembershipCreationFails() {
        long usersBefore = userRepository.count();
        long tenantsBefore = tenantRepository.count();
        long membershipsBefore = membershipRepository.count();

        String email = "rollback-" + UUID.randomUUID() + "@example.com";

        var request = new RegisterRequest(
                "Profissional de teste",
                email,
                "Uma senha longa para teste!",
                "Organização de teste"
        );

        doAnswer(invocation -> {
            // Executa no banco os INSERTs de usuário e organização.
            entityManager.flush();

            throw new IllegalStateException(
                    "Falha simulada ao criar vínculo."
            );
        }).when(membershipRepository).save(any(TenantMembership.class));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha simulada ao criar vínculo.");

        assertThat(userRepository.existsByEmail(email)).isFalse();
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(tenantRepository.count()).isEqualTo(tenantsBefore);
        assertThat(membershipRepository.count()).isEqualTo(membershipsBefore);
    }
}
