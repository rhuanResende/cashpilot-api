package com.desenvolvimento.logica.cashpilot_api.verification.model;

import com.desenvolvimento.logica.cashpilot_api.shared.model.BaseEntity;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 254, updatable = false)
    private String email;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64,
            updatable = false
    )
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(
            User user,
            String tokenHash,
            Instant expiresAt) {

        this.user = Objects.requireNonNull(
                user, "O usuário é obrigatório."
        );
        this.expiresAt = Objects.requireNonNull(
                expiresAt, "O vencimento é obrigatório."
        );

        if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Hash do token inválido.");
        }

        this.email = user.getEmail();
        this.tokenHash = tokenHash;
    }

    public boolean isUsableAt(Instant instant) {
        Objects.requireNonNull(instant, "O instante é obrigatório.");

        return consumedAt == null && instant.isBefore(expiresAt);
    }

    public void consume(Instant instant) {
        if (!isUsableAt(instant)) {
            throw new IllegalStateException(
                    "O token está expirado ou já foi utilizado."
            );
        }

        this.consumedAt = instant;
    }

    public User getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}
