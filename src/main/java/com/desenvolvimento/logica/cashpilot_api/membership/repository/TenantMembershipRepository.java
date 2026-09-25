package com.desenvolvimento.logica.cashpilot_api.membership.repository;

import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipStatus;
import com.desenvolvimento.logica.cashpilot_api.membership.model.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    Optional<TenantMembership> findByTenant_IdAndUser_Id(
            UUID tenantId,
            UUID userId
    );

    boolean existsByTenant_IdAndUser_Id(
            UUID tenantId,
            UUID userId
    );

    long countByTenant_IdAndStatus(
            UUID tenantId,
            MembershipStatus status
    );
}
