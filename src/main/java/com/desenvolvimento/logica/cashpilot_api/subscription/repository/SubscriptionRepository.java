package com.desenvolvimento.logica.cashpilot_api.subscription.repository;

import com.desenvolvimento.logica.cashpilot_api.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByTenant_Id(UUID tenantId);
}
