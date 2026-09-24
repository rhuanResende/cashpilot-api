package com.desenvolvimento.logica.cashpilot_api.trial.entity;

import com.desenvolvimento.logica.cashpilot_api.shared.entity.BaseEntity;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "tenant_trials")
public class TenantTrial extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tenant_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private Tenant tenant;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    protected TenantTrial() {
    }

    public TenantTrial(Tenant tenant, Instant startsAt) {
        this.tenant = Objects.requireNonNull(
                tenant, "A organização é obrigatória."
        );

        this.startsAt = Objects.requireNonNull(
                startsAt, "O início do teste é obrigatório."
        );

        this.endsAt = startsAt.plus(14, ChronoUnit.DAYS);
    }

    public boolean isActiveAt(Instant instant) {
        Objects.requireNonNull(instant, "O instante é obrigatório.");

        return !instant.isBefore(startsAt)
                && instant.isBefore(endsAt);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }
}
