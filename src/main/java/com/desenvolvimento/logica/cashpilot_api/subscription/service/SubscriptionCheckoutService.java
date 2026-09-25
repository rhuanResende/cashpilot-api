package com.desenvolvimento.logica.cashpilot_api.subscription.service;

import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipStatus;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.ForbiddenOperationException;
import com.desenvolvimento.logica.cashpilot_api.subscription.dto.SubscriptionReadinessResponse;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.TenantStatus;
import com.desenvolvimento.logica.cashpilot_api.user.model.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
public class SubscriptionCheckoutService {

    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final SubscriptionReadinessService readinessService;

    public SubscriptionCheckoutService(
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            SubscriptionReadinessService readinessService
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.readinessService = readinessService;
    }

    @Transactional(readOnly = true)
    public SubscriptionReadinessResponse checkReadiness(
            @NotNull UUID authenticatedUserId,
            @NotNull UUID tenantId) {

        var user = userRepository.findById(authenticatedUserId)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(ForbiddenOperationException::new);

        var membership = membershipRepository
                .findByTenant_IdAndUser_Id(tenantId, authenticatedUserId)
                .orElseThrow(ForbiddenOperationException::new);

        if (membership.getStatus() != MembershipStatus.ACTIVE
                || membership.getRole() != MembershipRole.OWNER) {
            throw new ForbiddenOperationException();
        }

        var tenant = membership.getTenant();

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ForbiddenOperationException();
        }

        return readinessService.check(user, tenant);
    }
}
