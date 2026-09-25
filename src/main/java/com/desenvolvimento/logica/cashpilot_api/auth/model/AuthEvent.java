package com.desenvolvimento.logica.cashpilot_api.auth.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_events")
public class AuthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            length = 64,
            updatable = false
    )
    private AuthEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuthEventOutcome outcome;

    @Column(name = "reason_code", length = 64, updatable = false)
    private String reasonCode;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "source_ip", length = 45, updatable = false)
    private String sourceIp;

    @Column(name = "user_agent", length = 512, updatable = false)
    private String userAgent;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    protected AuthEvent() {
    }

    public AuthEvent(
            Instant occurredAt,
            AuthEventType eventType,
            AuthEventOutcome outcome,
            String reasonCode,
            UUID userId,
            UUID actorUserId,
            UUID tenantId,
            String sourceIp,
            String userAgent,
            UUID requestId
    ) {
        this.occurredAt = Objects.requireNonNull(
                occurredAt, "O instante do evento é obrigatório"
        );
        this.eventType = Objects.requireNonNull(
                eventType, "O tipo do evento é obrigatório"
        );
        this.outcome = Objects.requireNonNull(
                outcome, "O resultado é obrigatório"
        );
        this.requestId = Objects.requireNonNull(
                requestId, "O identificador da requisição é obrigatório"
        );

        if (reasonCode != null
                && !reasonCode.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw new IllegalArgumentException(
                    "O código do motivo possui formato inválido"
            );
        }

        if (sourceIp != null
                && (sourceIp.isBlank() || sourceIp.length() > 45)) {
            throw new IllegalArgumentException(
                    "O endereço IP possui tamanho ou conteúdo inválido"
            );
        }

        this.reasonCode = reasonCode;
        this.userId = userId;
        this.actorUserId = actorUserId;
        this.tenantId = tenantId;
        this.sourceIp = sourceIp;
        this.userAgent = sanitizeUserAgent(userAgent);
    }

    private static String sanitizeUserAgent(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value
                .replaceAll("\\p{Cntrl}", " ")
                .strip();

        if (sanitized.isEmpty()) {
            return null;
        }

        return sanitized.substring(0, Math.min(sanitized.length(), 512));
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuthEventType getEventType() {
        return eventType;
    }

    public AuthEventOutcome getOutcome() {
        return outcome;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public UUID getRequestId() {
        return requestId;
    }
}
