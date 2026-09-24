package com.desenvolvimento.logica.cashpilot_api.user.entity;

import com.desenvolvimento.logica.cashpilot_api.shared.model.Address;
import com.desenvolvimento.logica.cashpilot_api.shared.model.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "document", length = 11)
    private String document;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false, length = 20)
    private final PlatformRole platformRole = PlatformRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Embedded
    private Address address;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = normalizeRequired(name, "Nome", 150);
        this.email = normalizeRequired(email, "E-mail", 254)
                .toLowerCase(Locale.ROOT);

        if (passwordHash == null || passwordHash.isBlank()
                || passwordHash.length() > 255) {
            throw new IllegalArgumentException("Hash da senha inválido.");
        }

        this.passwordHash = passwordHash;
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void markEmailVerified() {
        markEmailVerified(Instant.now());
    }

    public void markEmailVerified(Instant instant) {
        java.util.Objects.requireNonNull(
                instant, "O instante da confirmação é obrigatório."
        );

        if (this.emailVerifiedAt == null) {
            this.emailVerifiedAt = instant;
        }
    }

    public void updateProfile(
            String name,
            String document,
            String phone,
            Address address) {

        this.name = normalizeRequired(name, "Nome", 150);
        this.document = document;
        this.phone = phone;
        this.address = address;
    }

    private static String normalizeRequired(
            String value, String field, int maxLength) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " é obrigatório."
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " deve ter até " + maxLength + " caracteres."
            );
        }

        return normalized;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public PlatformRole getPlatformRole() {
        return platformRole;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Address getAddress() {
        return address;
    }
}
