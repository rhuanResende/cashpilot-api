package com.desenvolvimento.logica.cashpilot_api.registration.service;

import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.entity.TenantMembership;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterResponse;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.EmailAlreadyRegisteredException;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.trial.entity.TenantTrial;
import com.desenvolvimento.logica.cashpilot_api.trial.repository.TenantTrialRepository;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;

@Service
@Validated
public class RegistrationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantTrialRepository trialRepository;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            TenantTrialRepository trialRepository,
            Clock clock,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.trialRepository = trialRepository;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(
            @NotNull @Valid RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = userRepository.save(
                new User(
                        request.name(),
                        request.email(),
                        passwordHash
                )
        );

        Tenant tenant = tenantRepository.save(
                new Tenant(request.organizationName())
        );

        membershipRepository.save(
                new TenantMembership(
                        tenant,
                        user,
                        MembershipRole.OWNER
                )
        );

        trialRepository.save(
                new TenantTrial(
                        tenant,
                        Instant.now(clock)
                )
        );

        return new RegisterResponse(user.getId(), tenant.getId());
    }
}
