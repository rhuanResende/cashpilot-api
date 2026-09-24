package com.desenvolvimento.logica.cashpilot_api.verification;

import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.ResendVerificationRequest;
import com.desenvolvimento.logica.cashpilot_api.verification.service.ResendEmailVerificationService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ResendEmailVerificationConcurrencyIntegrationTest {

    @Autowired
    private ResendEmailVerificationService resendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void shouldIssueOnlyOneTokenForConcurrentRequests() throws Exception {
        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        String email = "resend-concurrent-" + UUID.randomUUID()
                + "@example.com";

        UUID userId = transactionTemplate.execute(status ->
                userRepository.save(
                        new User(
                                "Profissional de teste",
                                email,
                                "hash-ficticio-nao-utilizado-para-login"
                        )
                ).getId()
        );

        assertThat(userId).isNotNull();

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try {
            var first = executor.submit(() -> {
                resendWhenReleased(email, ready, start);
                return null;
            });

            var second = executor.submit(() -> {
                resendWhenReleased(email, ready, start);
                return null;
            });

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("As duas tarefas devem estar prontas")
                    .isTrue();

            // Libera as duas solicitações juntas.
            start.countDown();

            // Também propaga qualquer falha ocorrida nas tarefas.
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);

            Long tokenCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM email_verification_tokens
                    WHERE user_id = ?
                    """,
                    Long.class,
                    userId
            );

            assertThat(tokenCount).isEqualTo(1L);

            var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            verify(mailSender, times(1)).send(captor.capture());
            verifyNoMoreInteractions(mailSender);

            assertThat(captor.getValue().getTo()).containsExactly(email);
        } finally {
            start.countDown();
            executor.shutdownNow();

            boolean terminated =
                    executor.awaitTermination(10, TimeUnit.SECONDS);

            assertThat(terminated)
                    .as("As tarefas devem terminar antes da limpeza")
                    .isTrue();

            transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.update(
                        "DELETE FROM email_verification_tokens WHERE user_id = ?",
                        userId
                );

                jdbcTemplate.update(
                        "DELETE FROM users WHERE id = ?",
                        userId
                );
            });
        }
    }

    private void resendWhenReleased(
            String email,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Tempo esgotado aguardando início do reenvio."
            );
        }

        resendService.resend(new ResendVerificationRequest(email));
    }
}