package com.desenvolvimento.logica.cashpilot_api.plan.repository;

import com.desenvolvimento.logica.cashpilot_api.plan.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(String code);
}
