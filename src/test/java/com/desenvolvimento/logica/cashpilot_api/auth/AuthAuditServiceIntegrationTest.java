package com.desenvolvimento.logica.cashpilot_api.auth;

import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEvent;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventOutcome;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventType;
import com.desenvolvimento.logica.cashpilot_api.auth.repository.AuthEventRepository;
import com.desenvolvimento.logica.cashpilot_api.auth.service.AuthAuditService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AuthAuditServiceIntegrationTest {

    private static final Instant NOW =
            Instant.parse("2026-09-24T12:00:00Z");

    @Autowired
    private AuthAuditService auditService;

    @Autowired
    private AuthEventRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void configureClock() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void shouldPersistEventAfterCommit() {
        UUID requestId = UUID.randomUUID();

        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        try {
            transactionTemplate.executeWithoutResult(status ->
                    recordFailure(requestId)
            );

            var events =
                    repository.findAllByRequestIdOrderByOccurredAtAsc(requestId);

            assertThat(events).hasSize(1);

            AuthEvent event = events.getFirst();

            assertThat(event.getId()).isNotNull();
            assertThat(event.getOccurredAt()).isEqualTo(NOW);
            assertThat(event.getEventType())
                    .isEqualTo(AuthEventType.PASSWORD_CHECK);
            assertThat(event.getOutcome())
                    .isEqualTo(AuthEventOutcome.FAILURE);
            assertThat(event.getReasonCode())
                    .isEqualTo("INVALID_CREDENTIALS");
            assertThat(event.getRequestId()).isEqualTo(requestId);
            assertThat(event.getSourceIp()).isEqualTo("127.0.0.1");
        } finally {
            jdbcTemplate.update(
                    "DELETE FROM auth_events WHERE request_id = ?",
                    requestId
            );
        }
    }

    @Test
    void shouldRollbackEventWithTransaction() {
        UUID requestId = UUID.randomUUID();

        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            recordFailure(requestId);

            entityManager.flush();
            entityManager.clear();

            assertThat(
                    repository.findAllByRequestIdOrderByOccurredAtAsc(requestId)
            ).hasSize(1);

            status.setRollbackOnly();
        });

        assertThat(
                repository.findAllByRequestIdOrderByOccurredAtAsc(requestId)
        ).isEmpty();
    }

    @Test
    void shouldRejectCallWithoutTransaction() {
        UUID requestId = UUID.randomUUID();

        assertThatThrownBy(() -> recordFailure(requestId))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(
                repository.findAllByRequestIdOrderByOccurredAtAsc(requestId)
        ).isEmpty();
    }

    private void recordFailure(UUID requestId) {
        auditService.record(
                AuthEventType.PASSWORD_CHECK,
                AuthEventOutcome.FAILURE,
                "INVALID_CREDENTIALS",
                null,
                null,
                null,
                "127.0.0.1",
                "CashPilot integration test",
                requestId
        );
    }
}