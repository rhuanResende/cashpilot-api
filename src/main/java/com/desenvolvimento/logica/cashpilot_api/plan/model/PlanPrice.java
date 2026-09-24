package com.desenvolvimento.logica.cashpilot_api.plan.model;

import com.desenvolvimento.logica.cashpilot_api.shared.model.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "plan_prices")
public class PlanPrice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "billing_period",
            nullable = false,
            length = 20,
            updatable = false
    )
    private BillingPeriod billingPeriod;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    protected PlanPrice() {
    }

    public Plan getPlan() {
        return plan;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }
}
