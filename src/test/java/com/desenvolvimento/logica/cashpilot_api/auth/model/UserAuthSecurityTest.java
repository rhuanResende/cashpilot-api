package com.desenvolvimento.logica.cashpilot_api.auth.model;

import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthSecurityTest {

    private static final Instant START =
            Instant.parse("2026-09-24T12:00:00Z");

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);

    private UserAuthSecurity security;

    @BeforeEach
    void setUp() {
        security = new UserAuthSecurity(
                new User(
                        "Profissional de teste",
                        "usuario@example.com",
                        "hash-ficticio-nao-utilizado-para-login"
                )
        );
    }

    @Test
    void shouldStartWindowOnFirstFailure() {
        boolean locked = failAt(START);

        assertThat(locked).isFalse();
        assertThat(security.getPasswordFailedAttempts()).isEqualTo(1);
        assertThat(security.getPasswordFailureWindowStartedAt())
                .isEqualTo(START);
        assertThat(security.getLastPasswordFailureAt()).isEqualTo(START);
        assertThat(security.getPasswordLockedUntil()).isNull();
    }

    @Test
    void shouldLockOnFifthFailure() {
        for (int i = 0; i < 4; i++) {
            assertThat(failAt(START.plusSeconds(i))).isFalse();
        }

        Instant fifthFailure = START.plusSeconds(4);

        assertThat(failAt(fifthFailure)).isTrue();
        assertThat(security.getPasswordFailedAttempts()).isEqualTo(5);
        assertThat(security.getPasswordLockedUntil())
                .isEqualTo(fifthFailure.plus(LOCK));
    }

    @Test
    void shouldNotExtendActiveLock() {
        lockAccount();

        Instant lockedUntil = security.getPasswordLockedUntil();
        Instant lastFailure = security.getLastPasswordFailureAt();

        assertThat(failAt(START.plusSeconds(30))).isFalse();

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(5);
        assertThat(security.getPasswordLockedUntil()).isEqualTo(lockedUntil);
        assertThat(security.getLastPasswordFailureAt()).isEqualTo(lastFailure);
    }

    @Test
    void shouldKeepFailuresJustBeforeWindowExpires() {
        failAt(START);

        failAt(START.plus(WINDOW).minusNanos(1));

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(2);
        assertThat(security.getPasswordFailureWindowStartedAt())
                .isEqualTo(START);
    }

    @Test
    void shouldRestartCountExactlyWhenWindowExpires() {
        failAt(START);
        failAt(START.plusSeconds(1));

        Instant nextFailure = START.plus(WINDOW);

        assertThat(failAt(nextFailure)).isFalse();

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(1);
        assertThat(security.getPasswordFailureWindowStartedAt())
                .isEqualTo(nextFailure);
        assertThat(security.getLastPasswordFailureAt())
                .isEqualTo(nextFailure);
        assertThat(security.getPasswordLockedUntil()).isNull();
    }

    @Test
    void shouldUnlockExactlyAtLockExpiration() {
        lockAccount();

        Instant lockedUntil = security.getPasswordLockedUntil();

        assertThat(security.isPasswordLockedAt(lockedUntil.minusNanos(1)))
                .isTrue();

        assertThat(security.isPasswordLockedAt(lockedUntil)).isFalse();
    }

    @Test
    void shouldRestartCountAfterLockExpires() {
        lockAccount();

        Instant nextFailure = security.getPasswordLockedUntil();

        assertThat(failAt(nextFailure)).isFalse();

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(1);
        assertThat(security.getPasswordFailureWindowStartedAt())
                .isEqualTo(nextFailure);
        assertThat(security.getPasswordLockedUntil()).isNull();
    }

    @Test
    void shouldResetAttemptsAndPreserveLastFailure() {
        lockAccount();

        Instant lastFailure = security.getLastPasswordFailureAt();

        security.resetPasswordAttempts();

        assertThat(security.getPasswordFailedAttempts()).isZero();
        assertThat(security.getPasswordFailureWindowStartedAt()).isNull();
        assertThat(security.getPasswordLockedUntil()).isNull();
        assertThat(security.getLastPasswordFailureAt()).isEqualTo(lastFailure);
    }

    @Test
    void shouldStartMfaWindowOnFirstFailure() {
        assertThat(failMfaAt(START)).isFalse();

        assertThat(security.getMfaFailedAttempts()).isEqualTo(1);
        assertThat(security.getMfaFailureWindowStartedAt()).isEqualTo(START);
        assertThat(security.getLastMfaFailureAt()).isEqualTo(START);
        assertThat(security.getMfaLockedUntil()).isNull();
    }

    @Test
    void shouldLockMfaOnFifthFailure() {
        for (int i = 0; i < 4; i++) {
            assertThat(failMfaAt(START.plusSeconds(i))).isFalse();
        }

        Instant fifthFailure = START.plusSeconds(4);

        assertThat(failMfaAt(fifthFailure)).isTrue();
        assertThat(security.getMfaFailedAttempts()).isEqualTo(5);
        assertThat(security.getMfaLockedUntil())
                .isEqualTo(fifthFailure.plus(LOCK));
    }

    @Test
    void shouldNotExtendActiveMfaLock() {
        lockMfa();

        Instant lockedUntil = security.getMfaLockedUntil();
        Instant lastFailure = security.getLastMfaFailureAt();

        assertThat(failMfaAt(START.plusSeconds(30))).isFalse();

        assertThat(security.getMfaFailedAttempts()).isEqualTo(5);
        assertThat(security.getMfaLockedUntil()).isEqualTo(lockedUntil);
        assertThat(security.getLastMfaFailureAt()).isEqualTo(lastFailure);
    }

    @Test
    void shouldKeepMfaFailuresJustBeforeWindowExpires() {
        failMfaAt(START);

        failMfaAt(START.plus(WINDOW).minusNanos(1));

        assertThat(security.getMfaFailedAttempts()).isEqualTo(2);
        assertThat(security.getMfaFailureWindowStartedAt()).isEqualTo(START);
    }

    @Test
    void shouldRestartMfaCountExactlyWhenWindowExpires() {
        failMfaAt(START);
        failMfaAt(START.plusSeconds(1));

        Instant nextFailure = START.plus(WINDOW);

        assertThat(failMfaAt(nextFailure)).isFalse();

        assertThat(security.getMfaFailedAttempts()).isEqualTo(1);
        assertThat(security.getMfaFailureWindowStartedAt())
                .isEqualTo(nextFailure);
        assertThat(security.getLastMfaFailureAt()).isEqualTo(nextFailure);
        assertThat(security.getMfaLockedUntil()).isNull();
    }

    @Test
    void shouldUnlockMfaExactlyAtLockExpiration() {
        lockMfa();

        Instant lockedUntil = security.getMfaLockedUntil();

        assertThat(security.isMfaLockedAt(lockedUntil.minusNanos(1))).isTrue();
        assertThat(security.isMfaLockedAt(lockedUntil)).isFalse();
    }

    @Test
    void shouldRestartMfaCountAfterLockExpires() {
        lockMfa();

        Instant nextFailure = security.getMfaLockedUntil();

        assertThat(failMfaAt(nextFailure)).isFalse();

        assertThat(security.getMfaFailedAttempts()).isEqualTo(1);
        assertThat(security.getMfaFailureWindowStartedAt())
                .isEqualTo(nextFailure);
        assertThat(security.getMfaLockedUntil()).isNull();
    }

    @Test
    void shouldResetMfaAttemptsAndPreserveLastFailure() {
        lockMfa();

        Instant lastFailure = security.getLastMfaFailureAt();

        security.resetMfaAttempts();

        assertThat(security.getMfaFailedAttempts()).isZero();
        assertThat(security.getMfaFailureWindowStartedAt()).isNull();
        assertThat(security.getMfaLockedUntil()).isNull();
        assertThat(security.getLastMfaFailureAt()).isEqualTo(lastFailure);
    }

    @Test
    void shouldPreserveMfaLockWhenPasswordAttemptsAreReset() {
        lockAccount();
        lockMfa();

        Instant mfaLockedUntil = security.getMfaLockedUntil();

        security.resetPasswordAttempts();

        assertThat(security.getPasswordFailedAttempts()).isZero();
        assertThat(security.getPasswordLockedUntil()).isNull();

        assertThat(security.getMfaFailedAttempts()).isEqualTo(5);
        assertThat(security.getMfaLockedUntil()).isEqualTo(mfaLockedUntil);
    }

    @Test
    void shouldPreservePasswordLockWhenMfaAttemptsAreReset() {
        lockAccount();
        lockMfa();

        Instant passwordLockedUntil = security.getPasswordLockedUntil();

        security.resetMfaAttempts();

        assertThat(security.getMfaFailedAttempts()).isZero();
        assertThat(security.getMfaLockedUntil()).isNull();

        assertThat(security.getPasswordFailedAttempts()).isEqualTo(5);
        assertThat(security.getPasswordLockedUntil())
                .isEqualTo(passwordLockedUntil);
    }

    private boolean failAt(Instant now) {
        return security.registerPasswordFailure(
                now,
                MAX_ATTEMPTS,
                WINDOW,
                LOCK
        );
    }

    private void lockAccount() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            failAt(START.plusSeconds(i));
        }
    }

    private boolean failMfaAt(Instant now) {
        return security.registerMfaFailure(
                now,
                MAX_ATTEMPTS,
                WINDOW,
                LOCK
        );
    }

    private void lockMfa() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            failMfaAt(START.plusSeconds(i));
        }
    }
}