package com.desenvolvimento.logica.cashpilot_api.registration;

import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.service.RegistrationService;
import com.desenvolvimento.logica.cashpilot_api.verification.event.EmailVerificationRequestedEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(RegistrationEmailRollbackIntegrationTest.FailureConfig.class)
class RegistrationEmailRollbackIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void shouldRollbackEntireRegistrationWithoutSendingEmail() {
        Map<String, Long> countsBefore = countRows();

        String email = "rollback-email-" + UUID.randomUUID()
                + "@example.com";

        var request = new RegisterRequest(
                "Profissional de teste",
                email,
                "Uma senha longa para teste!",
                "Organização de teste"
        );

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha simulada antes do commit.");

        assertThat(countRows()).isEqualTo(countsBefore);

        Long usersWithEmail = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Long.class,
                email
        );

        assertThat(usersWithEmail).isZero();
        verifyNoInteractions(mailSender);
    }

    private Map<String, Long> countRows() {
        var counts = new LinkedHashMap<String, Long>();

        for (String table : List.of(
                "users",
                "tenants",
                "tenant_memberships",
                "subscriptions",
                "email_verification_tokens"
        )) {
            counts.put(
                    table,
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM " + table,
                            Long.class
                    )
            );
        }

        return counts;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureConfig {

        @Bean
        FailBeforeCommitListener failBeforeCommitListener(
                EntityManager entityManager,
                JdbcTemplate jdbcTemplate
        ) {
            return new FailBeforeCommitListener(
                    entityManager,
                    jdbcTemplate
            );
        }
    }

    static class FailBeforeCommitListener {

        private final EntityManager entityManager;
        private final JdbcTemplate jdbcTemplate;

        FailBeforeCommitListener(
                EntityManager entityManager,
                JdbcTemplate jdbcTemplate
        ) {
            this.entityManager = entityManager;
            this.jdbcTemplate = jdbcTemplate;
        }

        @TransactionalEventListener(
                phase = TransactionPhase.BEFORE_COMMIT
        )
        public void onVerificationRequested(
                EmailVerificationRequestedEvent event
        ) {
            entityManager.flush();

            Long tokensCreated = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM email_verification_tokens t
                    JOIN users u ON u.id = t.user_id
                    WHERE u.email = ?
                    """,
                    Long.class,
                    event.verification().email()
            );

            assertThat(tokensCreated).isEqualTo(1L);

            throw new IllegalStateException(
                    "Falha simulada antes do commit."
            );
        }
    }
}