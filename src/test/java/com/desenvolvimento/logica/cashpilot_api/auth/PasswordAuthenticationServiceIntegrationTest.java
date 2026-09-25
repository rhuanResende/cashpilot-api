package com.desenvolvimento.logica.cashpilot_api.auth;

import com.desenvolvimento.logica.cashpilot_api.auth.dto.AuthenticationRequestContext;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.PasswordAuthenticationResult;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventOutcome;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventType;
import com.desenvolvimento.logica.cashpilot_api.auth.model.PasswordAuthenticationStatus;
import com.desenvolvimento.logica.cashpilot_api.auth.model.UserAuthSecurity;
import com.desenvolvimento.logica.cashpilot_api.auth.repository.AuthEventRepository;
import com.desenvolvimento.logica.cashpilot_api.auth.repository.UserAuthSecurityRepository;
import com.desenvolvimento.logica.cashpilot_api.auth.service.AuthAuditService;
import com.desenvolvimento.logica.cashpilot_api.auth.service.PasswordAuthenticationService;
import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "app.security.authentication.password.max-failed-attempts=5",
                "app.security.authentication.password.failure-window=15m",
                "app.security.authentication.password.lock-duration=15m"
        }
)
@ActiveProfiles("test")
class PasswordAuthenticationServiceIntegrationTest {

    private static final Instant NOW =
            Instant.parse("2026-09-24T12:00:00Z");

    private static final String PASSWORD =
            "Uma senha longa para teste!";

    @MockitoSpyBean
    private AuthAuditService auditService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordAuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthSecurityRepository securityRepository;

    @Autowired
    private AuthEventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private JavaMailSender mailSender;

    private UUID userId;
    private String email;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);

        email = "password-auth-" + UUID.randomUUID() + "@example.com";

        String passwordHash = passwordEncoder.encode(PASSWORD);

        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        userId = transactionTemplate.execute(status -> {
            User user = new User(
                    "Profissional de teste",
                    email,
                    passwordHash
            );

            user.markEmailVerified(NOW);
            userRepository.save(user);

            securityRepository.save(new UserAuthSecurity(user));

            return user.getId();
        });
    }

    @AfterEach
    void cleanUp() {
        if (userId == null) {
            return;
        }

        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "DELETE FROM auth_events WHERE user_id = ?",
                    userId
            );

            jdbcTemplate.update(
                    "DELETE FROM user_auth_security WHERE user_id = ?",
                    userId
            );

            jdbcTemplate.update(
                    "DELETE FROM users WHERE id = ?",
                    userId
            );
        });
    }

    @Test
    void shouldCommitFailureCounterAndAuditWhenPasswordIsWrong() {
        var context = context();

        var result = authenticationService.authenticate(
                email,
                "Senha incorreta!",
                context
        );

        assertThat(result.status())
                .isEqualTo(PasswordAuthenticationStatus.DENIED);
        assertThat(result.userId()).isNull();

        var security = securityRepository.findById(userId).orElseThrow();

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(1);
        assertThat(security.getLastPasswordFailureAt()).isEqualTo(NOW);
        assertThat(security.getPasswordLockedUntil()).isNull();

        var events = eventRepository
                .findAllByRequestIdOrderByOccurredAtAsc(context.requestId());

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType())
                .isEqualTo(AuthEventType.PASSWORD_CHECK);
        assertThat(events.getFirst().getOutcome())
                .isEqualTo(AuthEventOutcome.FAILURE);
        assertThat(events.getFirst().getReasonCode())
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void shouldLockAfterFiveFailuresAndRejectCorrectPasswordWhileLocked() {
        var fifthContext = context();

        for (int i = 0; i < 5; i++) {
            var result = authenticationService.authenticate(
                    email,
                    "Senha incorreta!",
                    i == 4 ? fifthContext : context()
            );

            assertThat(result.status())
                    .isEqualTo(PasswordAuthenticationStatus.DENIED);
        }

        var fifthEvents = eventRepository
                .findAllByRequestIdOrderByOccurredAtAsc(
                        fifthContext.requestId()
                );

        assertThat(fifthEvents)
                .extracting(event -> event.getEventType())
                .containsExactlyInAnyOrder(
                        AuthEventType.PASSWORD_CHECK,
                        AuthEventType.TEMPORARY_LOCK_APPLIED
                );

        var blockedContext = context();

        var result = authenticationService.authenticate(
                email,
                PASSWORD,
                blockedContext
        );

        assertThat(result.status())
                .isEqualTo(PasswordAuthenticationStatus.DENIED);

        var security = securityRepository.findById(userId).orElseThrow();

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(5);
        assertThat(security.getPasswordLockedUntil())
                .isEqualTo(NOW.plusSeconds(900));

        var blockedEvents = eventRepository
                .findAllByRequestIdOrderByOccurredAtAsc(
                        blockedContext.requestId()
                );

        assertThat(blockedEvents).hasSize(1);
        assertThat(blockedEvents.getFirst().getOutcome())
                .isEqualTo(AuthEventOutcome.DENIED);
        assertThat(blockedEvents.getFirst().getReasonCode())
                .isEqualTo("TEMPORARILY_LOCKED");
    }

    @Test
    void shouldResetPasswordFailuresWithoutCompletingLogin() {
        authenticationService.authenticate(
                email,
                "Senha incorreta!",
                context()
        );

        var successContext = context();

        var result = authenticationService.authenticate(
                email,
                PASSWORD,
                successContext
        );

        assertThat(result.status())
                .isEqualTo(PasswordAuthenticationStatus.PASSWORD_VERIFIED);
        assertThat(result.userId()).isEqualTo(userId);

        var security = securityRepository.findById(userId).orElseThrow();

        assertThat(security.getPasswordFailedAttempts()).isZero();
        assertThat(security.getPasswordFailureWindowStartedAt()).isNull();
        assertThat(security.getPasswordLockedUntil()).isNull();

        var user = userRepository.findById(userId).orElseThrow();

        assertThat(user.getLastLoginAt()).isNull();

        var events = eventRepository
                .findAllByRequestIdOrderByOccurredAtAsc(
                        successContext.requestId()
                );

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType())
                .isEqualTo(AuthEventType.PASSWORD_CHECK);
        assertThat(events.getFirst().getOutcome())
                .isEqualTo(AuthEventOutcome.SUCCESS);
    }

    @Test
    void shouldDenyUnknownEmailAndRecordFailure() {
        var requestContext = context();

        try {
            var result = authenticationService.authenticate(
                    "unknown-" + UUID.randomUUID() + "@example.com",
                    PASSWORD,
                    requestContext
            );

            assertThat(result.status())
                    .isEqualTo(PasswordAuthenticationStatus.DENIED);
            assertThat(result.userId()).isNull();

            var events = eventRepository
                    .findAllByRequestIdOrderByOccurredAtAsc(
                            requestContext.requestId()
                    );

            assertThat(events).hasSize(1);

            var event = events.getFirst();

            assertThat(event.getUserId()).isNull();
            assertThat(event.getEventType())
                    .isEqualTo(AuthEventType.PASSWORD_CHECK);
            assertThat(event.getOutcome())
                    .isEqualTo(AuthEventOutcome.FAILURE);
            assertThat(event.getReasonCode())
                    .isEqualTo("INVALID_CREDENTIALS");
        } finally {
            // O evento não possui user_id, então limpamos pelo request_id.
            jdbcTemplate.update(
                    "DELETE FROM auth_events WHERE request_id = ?",
                    requestContext.requestId()
            );
        }
    }

    @Test
    void shouldDenyAdministrativelyBlockedUser() {
        var transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            var user = userRepository.findById(userId).orElseThrow();
            user.block();
        });

        var requestContext = context();

        var result = authenticationService.authenticate(
                email,
                PASSWORD,
                requestContext
        );

        assertThat(result.status())
                .isEqualTo(PasswordAuthenticationStatus.DENIED);
        assertThat(result.userId()).isNull();

        var security = securityRepository.findById(userId).orElseThrow();

        assertThat(security.getPasswordFailedAttempts()).isZero();
        assertThat(security.getPasswordLockedUntil()).isNull();

        var events = eventRepository
                .findAllByRequestIdOrderByOccurredAtAsc(
                        requestContext.requestId()
                );

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getOutcome())
                .isEqualTo(AuthEventOutcome.DENIED);
        assertThat(events.getFirst().getReasonCode())
                .isEqualTo("ACCOUNT_BLOCKED");

        assertThat(
                userRepository.findById(userId).orElseThrow().getLastLoginAt()
        ).isNull();
    }

    @Test
    void shouldDenyUnverifiedEmailEvenWithCorrectPassword() {
        // Ajusta somente o usuário criado para este teste.
        jdbcTemplate.update(
                "UPDATE users SET email_verified_at = NULL WHERE id = ?",
                userId
        );

        var requestContext = context();

        var result = authenticationService.authenticate(
                email,
                PASSWORD,
                requestContext
        );

        assertThat(result.status())
                .isEqualTo(PasswordAuthenticationStatus.DENIED);
        assertThat(result.userId()).isNull();

        var events = eventRepository
                .findAllByRequestIdOrderByOccurredAtAsc(
                        requestContext.requestId()
                );

        assertThat(events).hasSize(2);

        assertThat(events).anySatisfy(event -> {
            assertThat(event.getEventType())
                    .isEqualTo(AuthEventType.PASSWORD_CHECK);
            assertThat(event.getOutcome())
                    .isEqualTo(AuthEventOutcome.SUCCESS);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.getEventType())
                    .isEqualTo(AuthEventType.ACCESS_DENIED);
            assertThat(event.getOutcome())
                    .isEqualTo(AuthEventOutcome.DENIED);
            assertThat(event.getReasonCode())
                    .isEqualTo("EMAIL_NOT_VERIFIED");
        });

        assertThat(
                userRepository.findById(userId).orElseThrow().getLastLoginAt()
        ).isNull();
    }

    @Test
    void shouldRollbackCounterAndEventWhenAuditFails() {
        var requestContext = context();

        AuthAuditService auditSpy =
                AopTestUtils.getUltimateTargetObject(auditService);

        doAnswer(invocation -> {
            invocation.callRealMethod();

            entityManager.flush();

            throw new IllegalStateException(
                    "Falha simulada na auditoria."
            );
        }).when(auditSpy).record(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        assertThatThrownBy(() ->
                authenticationService.authenticate(
                        email,
                        "Senha incorreta!",
                        requestContext
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha simulada na auditoria.");

        var security = securityRepository.findById(userId).orElseThrow();

        assertThat(security.getPasswordFailedAttempts()).isZero();
        assertThat(security.getPasswordFailureWindowStartedAt()).isNull();
        assertThat(security.getPasswordLockedUntil()).isNull();
        assertThat(security.getLastPasswordFailureAt()).isNull();

        assertThat(
                eventRepository.findAllByRequestIdOrderByOccurredAtAsc(
                        requestContext.requestId()
                )
        ).isEmpty();

        assertThat(
                userRepository.findById(userId).orElseThrow().getLastLoginAt()
        ).isNull();
    }

    @Test
    void shouldCountConcurrentFailuresAndApplyLockOnlyOnce() throws Exception {
        int attempts = 5;

        var executor = Executors.newFixedThreadPool(attempts);
        var ready = new CountDownLatch(attempts);
        var start = new CountDownLatch(1);

        List<AuthenticationRequestContext> contexts = new ArrayList<>();
        List<Future<PasswordAuthenticationResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                var requestContext = context();
                contexts.add(requestContext);

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Tempo esgotado aguardando início das tentativas."
                        );
                    }

                    return authenticationService.authenticate(
                            email,
                            "Senha incorreta!",
                            requestContext
                    );
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("Todas as tarefas devem estar prontas")
                    .isTrue();

            start.countDown();

            for (var future : futures) {
                var result = future.get(30, TimeUnit.SECONDS);

                assertThat(result.status())
                        .isEqualTo(PasswordAuthenticationStatus.DENIED);
                assertThat(result.userId()).isNull();
            }

            var security = securityRepository.findById(userId).orElseThrow();

            assertThat(security.getPasswordFailedAttempts()).isEqualTo(5);
            assertThat(security.getPasswordFailureWindowStartedAt())
                    .isEqualTo(NOW);
            assertThat(security.getLastPasswordFailureAt()).isEqualTo(NOW);
            assertThat(security.getPasswordLockedUntil())
                    .isEqualTo(NOW.plusSeconds(900));

            var events = contexts.stream()
                    .flatMap(requestContext ->
                            eventRepository
                                    .findAllByRequestIdOrderByOccurredAtAsc(
                                            requestContext.requestId()
                                    )
                                    .stream()
                    )
                    .toList();

            assertThat(events).hasSize(6);

            assertThat(events.stream()
                    .filter(event ->
                            event.getEventType() == AuthEventType.PASSWORD_CHECK
                    )
                    .toList())
                    .hasSize(5)
                    .allSatisfy(event -> {
                        assertThat(event.getOutcome())
                                .isEqualTo(AuthEventOutcome.FAILURE);
                        assertThat(event.getReasonCode())
                                .isEqualTo("INVALID_CREDENTIALS");
                    });

            assertThat(events.stream()
                    .filter(event ->
                            event.getEventType()
                                    == AuthEventType.TEMPORARY_LOCK_APPLIED
                    )
                    .toList())
                    .hasSize(1)
                    .allSatisfy(event -> {
                        assertThat(event.getOutcome())
                                .isEqualTo(AuthEventOutcome.SUCCESS);
                        assertThat(event.getReasonCode())
                                .isEqualTo("PASSWORD_ATTEMPT_LIMIT");
                    });
        } finally {
            start.countDown();
            executor.shutdownNow();

            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .as("As tarefas devem terminar antes da limpeza do banco")
                    .isTrue();
        }
    }

    private AuthenticationRequestContext context() {
        return new AuthenticationRequestContext(
                UUID.randomUUID(),
                "127.0.0.1",
                "CashPilot integration test"
        );
    }
}