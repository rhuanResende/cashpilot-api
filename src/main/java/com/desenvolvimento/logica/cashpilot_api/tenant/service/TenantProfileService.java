package com.desenvolvimento.logica.cashpilot_api.tenant.service;

import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipStatus;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.ForbiddenOperationException;
import com.desenvolvimento.logica.cashpilot_api.shared.mapper.AddressMapper;
import com.desenvolvimento.logica.cashpilot_api.tenant.dto.UpdateTenantProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.TenantStatus;
import com.desenvolvimento.logica.cashpilot_api.user.model.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
public class TenantProfileService {

    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;

    public TenantProfileService(
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository) {

        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public void updateProfile(
            @NotNull UUID authenticatedUserId,
            @NotNull UUID tenantId,
            @NotNull @Valid UpdateTenantProfileRequest request) {

        userRepository.findById(authenticatedUserId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(ForbiddenOperationException::new);

        var membership = membershipRepository
                .findByTenant_IdAndUser_Id(tenantId, authenticatedUserId)
                .orElseThrow(ForbiddenOperationException::new);

        boolean canEdit =
                membership.getRole() == MembershipRole.OWNER
                        || membership.getRole() == MembershipRole.ADMIN;

        if (membership.getStatus() != MembershipStatus.ACTIVE || !canEdit) {
            throw new ForbiddenOperationException();
        }

        Tenant tenant = membership.getTenant();

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ForbiddenOperationException();
        }

        tenant.updateProfile(
                request.name(),
                request.legalName(),
                request.documentType(),
                request.document(),
                request.contactEmail(),
                request.phone(),
                AddressMapper.toEntity(request.address())
        );
    }
}
