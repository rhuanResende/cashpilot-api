package com.desenvolvimento.logica.cashpilot_api.auth.model;

import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_auth_security")
public class UserAuthSecurity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "password_failed_attempts", nullable = false)
    private int passwordFailedAttempts;

    @Column(name = "password_failure_window_started_at")
    private Instant passwordFailureWindowStartedAt;

    @Column(name = "password_locked_until")
    private Instant passwordLockedUntil;

    @Column(name = "last_password_failure_at")
    private Instant lastPasswordFailureAt;

    @Column(name = "mfa_failed_attempts", nullable = false)
    private int mfaFailedAttempts;

    @Column(name = "mfa_failure_window_started_at")
    private Instant mfaFailureWindowStartedAt;

    @Column(name = "mfa_locked_until")
    private Instant mfaLockedUntil;

    @Column(name = "last_mfa_failure_at")
    private Instant lastMfaFailureAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAuthSecurity() {
    }

    public UserAuthSecurity(User user) {
        this.user = Objects.requireNonNull(
                user,
                "O usuário é obrigatório"
        );
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isPasswordLockedAt(Instant now) {
        Objects.requireNonNull(now, "O instante é obrigatório");

        return passwordLockedUntil != null
                && now.isBefore(passwordLockedUntil);
    }

    public boolean isMfaLockedAt(Instant now) {
        Objects.requireNonNull(now, "O instante é obrigatório");

        return mfaLockedUntil != null
                && now.isBefore(mfaLockedUntil);
    }

    public boolean registerPasswordFailure(
            Instant now,
            int maxFailedAttempts,
            Duration failureWindow,
            Duration lockDuration
    ) {
        Objects.requireNonNull(now, "O instante é obrigatório");
        requirePositive(failureWindow, "A janela de tentativas");
        requirePositive(lockDuration, "A duração do bloqueio");

        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException(
                    "O limite de tentativas deve ser maior que zero"
            );
        }

        // Tentativas durante o bloqueio não aumentam nem prolongam a punição.
        if (isPasswordLockedAt(now)) {
            return false;
        }

        boolean previousLockExpired =
                passwordLockedUntil != null
                        && !now.isBefore(passwordLockedUntil);

        boolean failureWindowExpired =
                passwordFailureWindowStartedAt != null
                        && !now.isBefore(
                        passwordFailureWindowStartedAt.plus(failureWindow)
                );

        if (previousLockExpired || failureWindowExpired) {
            resetPasswordAttempts();
        }

        if (passwordFailureWindowStartedAt == null) {
            passwordFailureWindowStartedAt = now;
        }

        passwordFailedAttempts++;
        lastPasswordFailureAt = now;

        if (passwordFailedAttempts >= maxFailedAttempts) {
            passwordLockedUntil = now.plus(lockDuration);
            return true;
        }

        return false;
    }

    public void resetPasswordAttempts() {
        passwordFailedAttempts = 0;
        passwordFailureWindowStartedAt = null;
        passwordLockedUntil = null;
    }

    private static void requirePositive(Duration value, String field) {
        Objects.requireNonNull(value, field + " é obrigatória");

        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    field + " deve ser positiva"
            );
        }
    }

    public boolean registerMfaFailure(
            Instant now,
            int maxFailedAttempts,
            Duration failureWindow,
            Duration lockDuration
    ) {
        Objects.requireNonNull(now, "O instante é obrigatório");
        requirePositive(failureWindow, "A janela de tentativas");
        requirePositive(lockDuration, "A duração do bloqueio");

        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException(
                    "O limite de tentativas deve ser maior que zero"
            );
        }

        if (isMfaLockedAt(now)) {
            return false;
        }

        boolean previousLockExpired =
                mfaLockedUntil != null
                        && !now.isBefore(mfaLockedUntil);

        boolean failureWindowExpired =
                mfaFailureWindowStartedAt != null
                        && !now.isBefore(
                        mfaFailureWindowStartedAt.plus(failureWindow)
                );

        if (previousLockExpired || failureWindowExpired) {
            resetMfaAttempts();
        }

        if (mfaFailureWindowStartedAt == null) {
            mfaFailureWindowStartedAt = now;
        }

        mfaFailedAttempts++;
        lastMfaFailureAt = now;

        if (mfaFailedAttempts >= maxFailedAttempts) {
            mfaLockedUntil = now.plus(lockDuration);
            return true;
        }

        return false;
    }

    public void resetMfaAttempts() {
        mfaFailedAttempts = 0;
        mfaFailureWindowStartedAt = null;
        mfaLockedUntil = null;
    }

    public UUID getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public int getPasswordFailedAttempts() {
        return passwordFailedAttempts;
    }

    public Instant getPasswordFailureWindowStartedAt() {
        return passwordFailureWindowStartedAt;
    }

    public Instant getPasswordLockedUntil() {
        return passwordLockedUntil;
    }

    public Instant getLastPasswordFailureAt() {
        return lastPasswordFailureAt;
    }

    public int getMfaFailedAttempts() {
        return mfaFailedAttempts;
    }

    public Instant getMfaFailureWindowStartedAt() {
        return mfaFailureWindowStartedAt;
    }

    public Instant getMfaLockedUntil() {
        return mfaLockedUntil;
    }

    public Instant getLastMfaFailureAt() {
        return lastMfaFailureAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
