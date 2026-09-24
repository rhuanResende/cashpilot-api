package com.desenvolvimento.logica.cashpilot_api.plan.repository;

import com.desenvolvimento.logica.cashpilot_api.plan.model.BillingPeriod;
import com.desenvolvimento.logica.cashpilot_api.plan.model.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, UUID> {
    List<PlanPrice> findAllByPlan_IdAndActiveTrue(UUID planId);

    Optional<PlanPrice> findByPlan_IdAndBillingPeriodAndCurrencyAndActiveTrue(
            UUID planId,
            BillingPeriod billingPeriod,
            String currency
    );
}
