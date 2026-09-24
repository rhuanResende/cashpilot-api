package com.desenvolvimento.logica.cashpilot_api.trial.repository;

import com.desenvolvimento.logica.cashpilot_api.trial.entity.TenantTrial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantTrialRepository extends JpaRepository<TenantTrial, UUID> {
    Optional<TenantTrial> findByTenant_Id(UUID tenantId);
}
