package com.desenvolvimento.logica.cashpilot_api.tenant.repository;

import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
