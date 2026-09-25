package com.desenvolvimento.logica.cashpilot_api.subscription.model;

import com.desenvolvimento.logica.cashpilot_api.plan.model.Plan;
import com.desenvolvimento.logica.cashpilot_api.plan.model.PlanPrice;
import com.desenvolvimento.logica.cashpilot_api.shared.model.BaseEntity;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true,
            updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_price_id")
    private PlanPrice planPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "current_period_starts_at", nullable = false)
    private Instant currentPeriodStartsAt;

    @Column(name = "current_period_ends_at", nullable = false)
    private Instant currentPeriodEndsAt;

    @Column(name = "trial_starts_at", updatable = false)
    private Instant trialStartsAt;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "cancellation_requested_at")
    private Instant cancellationRequestedAt;

    protected Subscription() {
    }

    public static Subscription startTrial(
            Tenant tenant,
            Plan plan,
            Instant startsAt) {

        Objects.requireNonNull(tenant, "A organização é obrigatória.");
        Objects.requireNonNull(plan, "O plano é obrigatório.");
        Objects.requireNonNull(startsAt, "O início é obrigatório.");

        if (!"TRIAL".equals(plan.getCode()) || !plan.isActive()) {
            throw new IllegalArgumentException(
                    "O plano de teste deve ser TRIAL e estar ativo."
            );
        }

        Integer duration = plan.getTrialDurationDays();

        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException(
                    "O plano de teste deve ter uma duração válida."
            );
        }

        Subscription subscription = new Subscription();
        subscription.tenant = tenant;
        subscription.plan = plan;
        subscription.status = SubscriptionStatus.TRIALING;
        subscription.trialStartsAt = startsAt;
        subscription.trialEndsAt = startsAt.plus(duration, ChronoUnit.DAYS);
        subscription.currentPeriodStartsAt = subscription.trialStartsAt;
        subscription.currentPeriodEndsAt = subscription.trialEndsAt;

        return subscription;
    }

    public boolean hasAccessAt(Instant instant) {
        Objects.requireNonNull(instant, "O instante é obrigatório.");

        boolean eligibleStatus =
                status == SubscriptionStatus.TRIALING
                        || status == SubscriptionStatus.ACTIVE;

        return eligibleStatus
                && !instant.isBefore(currentPeriodStartsAt)
                && instant.isBefore(currentPeriodEndsAt);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Plan getPlan() {
        return plan;
    }

    public PlanPrice getPlanPrice() {
        return planPrice;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCurrentPeriodStartsAt() {
        return currentPeriodStartsAt;
    }

    public Instant getCurrentPeriodEndsAt() {
        return currentPeriodEndsAt;
    }

    public Instant getTrialStartsAt() {
        return trialStartsAt;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCancellationRequestedAt() {
        return cancellationRequestedAt;
    }
}
