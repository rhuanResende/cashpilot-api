package com.desenvolvimento.logica.cashpilot_api.plan.model;

import com.desenvolvimento.logica.cashpilot_api.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_products")
    private Integer maxProducts;

    @Column(name = "trial_duration_days")
    private Integer trialDurationDays;

    @Column(nullable = false)
    private boolean active;

    protected Plan() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public Integer getMaxProducts() {
        return maxProducts;
    }

    public Integer getTrialDurationDays() {
        return trialDurationDays;
    }

    public boolean isActive() {
        return active;
    }
}
