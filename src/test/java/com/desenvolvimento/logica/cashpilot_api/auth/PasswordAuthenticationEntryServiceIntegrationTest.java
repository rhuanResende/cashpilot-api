package com.desenvolvimento.logica.cashpilot_api.auth;

import com.desenvolvimento.logica.cashpilot_api.auth.dto.LoginRateLimitResult;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.PasswordAuthenticationResult;
import com.desenvolvimento.logica.cashpilot_api.auth.service.LoginIpRateLimiter;
import com.desenvolvimento.logica.cashpilot_api.auth.service.PasswordAuthenticationEntryService;
import com.desenvolvimento.logica.cashpilot_api.auth.service.PasswordAuthenticationService;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.LoginRateLimitUnavailableException;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PasswordAuthenticationEntryServiceIntegrationTest {

    private static final String EMAIL = "usuario@example.com";
    private static final String PASSWORD = "Uma senha longa para teste!";

    @Autowired
    private PasswordAuthenticationEntryService entryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private LoginIpRateLimiter rateLimiter;

    @MockitoBean
    private PasswordAuthenticationService authenticationService;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockHttpServletRequest request;
    private String userAgent;

    @BeforeEach
    void setUp() {
        userAgent = "CashPilot-entry-test-" + UUID.randomUUID();

        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("User-Agent", userAgent);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM auth_events WHERE user_agent = ?",
                userAgent
        );
    }

    @Test
    void shouldCheckRateLimitBeforePasswordWithoutOpenTransaction() {
        var expected = PasswordAuthenticationResult.verified(
                UUID.randomUUID()
        );

        when(rateLimiter.consume(any())).thenAnswer(invocation -> {
            assertThat(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            ).isFalse();

            return new LoginRateLimitResult(
                    true,
                    19,
                    Duration.ZERO
            );
        });

        when(authenticationService.authenticate(
                eq(EMAIL),
                eq(PASSWORD),
                any()
        )).thenReturn(expected);

        var result = entryService.authenticate(
                EMAIL,
                PASSWORD,
                request
        );

        assertThat(result).isEqualTo(expected);

        var order = inOrder(rateLimiter, authenticationService);

        order.verify(rateLimiter).consume(
                argThat(address ->
                        address.getHostAddress().equals("192.0.2.10")
                )
        );

        order.verify(authenticationService).authenticate(
                eq(EMAIL),
                eq(PASSWORD),
                argThat(context ->
                        context.requestId() != null
                                && context.sourceIp().equals("192.0.2.10")
                                && context.userAgent().equals(userAgent)
                )
        );

        order.verifyNoMoreInteractions();
    }

    @Test
    void shouldAuditRateLimitDenialWithoutCheckingPassword() {
        when(rateLimiter.consume(any())).thenReturn(
                new LoginRateLimitResult(
                        false,
                        0,
                        Duration.ofMillis(1_250)
                )
        );

        assertThatThrownBy(() ->
                entryService.authenticate(EMAIL, PASSWORD, request)
        ).isInstanceOfSatisfying(
                TooManyLoginAttemptsException.class,
                exception -> assertThat(exception.getRetryAfterSeconds())
                        .isEqualTo(2)
        );

        verifyNoInteractions(authenticationService);

        assertDenialWasCommitted("IP_RATE_LIMIT_EXCEEDED");
    }

    @Test
    void shouldAuditRedisFailureWithoutCheckingPassword() {
        var failure = new LoginRateLimitUnavailableException();

        when(rateLimiter.consume(any())).thenThrow(failure);

        assertThatThrownBy(() ->
                entryService.authenticate(EMAIL, PASSWORD, request)
        ).isSameAs(failure);

        verifyNoInteractions(authenticationService);

        assertDenialWasCommitted("RATE_LIMIT_UNAVAILABLE");
    }

    private void assertDenialWasCommitted(String reasonCode) {
        var reasons = jdbcTemplate.queryForList(
                """
                SELECT reason_code
                FROM auth_events
                WHERE user_agent = ?
                  AND event_type = 'ACCESS_DENIED'
                  AND outcome = 'DENIED'
                  AND source_ip = '192.0.2.10'
                  AND user_id IS NULL
                """,
                String.class,
                userAgent
        );

        assertThat(reasons).containsExactly(reasonCode);
    }
}