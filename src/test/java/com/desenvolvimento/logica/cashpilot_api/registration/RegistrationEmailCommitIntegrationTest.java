package com.desenvolvimento.logica.cashpilot_api.registration;

import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterResponse;
import com.desenvolvimento.logica.cashpilot_api.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class RegistrationEmailCommitIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void shouldSendExactlyOneEmailOnlyAfterCommit() {
        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        String email = "commit-email-" + UUID.randomUUID()
                + "@example.com";

        var request = new RegisterRequest(
                "Profissional de teste",
                email,
                "Uma senha longa para teste!",
                "Organização de teste"
        );

        RegisterResponse response = transactionTemplate.execute(status -> {
            RegisterResponse registration =
                    registrationService.register(request);

            // O cadastro terminou, mas a transação ainda está aberta.
            verifyNoInteractions(mailSender);

            return registration;
        });

        assertThat(response).isNotNull();

        try {
            // Neste ponto, a transação já foi confirmada.
            var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            verify(mailSender, times(1)).send(captor.capture());
            verifyNoMoreInteractions(mailSender);

            SimpleMailMessage message = captor.getValue();

            assertThat(message.getTo()).containsExactly(email);

            Long savedTokens = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM email_verification_tokens
                    WHERE user_id = ?
                    """,
                    Long.class,
                    response.userId()
            );

            assertThat(savedTokens).isEqualTo(1L);
        } finally {
            // O teste fez commit real: remove apenas os dados que criou.
            transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.update(
                        "DELETE FROM email_verification_tokens WHERE user_id = ?",
                        response.userId()
                );

                jdbcTemplate.update(
                        "DELETE FROM tenant_memberships WHERE tenant_id = ?",
                        response.tenantId()
                );

                jdbcTemplate.update(
                        "DELETE FROM subscriptions WHERE tenant_id = ?",
                        response.tenantId()
                );

                jdbcTemplate.update(
                        "DELETE FROM tenants WHERE id = ?",
                        response.tenantId()
                );

                jdbcTemplate.update(
                        "DELETE FROM users WHERE id = ?",
                        response.userId()
                );
            });
        }
    }
}