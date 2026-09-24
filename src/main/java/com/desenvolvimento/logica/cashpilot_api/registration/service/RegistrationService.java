package com.desenvolvimento.logica.cashpilot_api.registration.service;

import com.desenvolvimento.logica.cashpilot_api.membership.entity.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.entity.TenantMembership;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.plan.model.Plan;
import com.desenvolvimento.logica.cashpilot_api.plan.repository.PlanRepository;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterResponse;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.EmailAlreadyRegisteredException;
import com.desenvolvimento.logica.cashpilot_api.subscription.model.Subscription;
import com.desenvolvimento.logica.cashpilot_api.subscription.repository.SubscriptionRepository;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
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
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            Clock clock,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
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

        var trialPlan = planRepository.findByCode("TRIAL")
                .filter(Plan::isActive)
                .orElseThrow(() -> new IllegalStateException(
                        "O plano TRIAL não está disponível para cadastro."
                ));

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

        subscriptionRepository.save(
                Subscription.startTrial(
                        tenant,
                        trialPlan,
                        Instant.now(clock)
                )
        );

        return new RegisterResponse(
                user.getId(),
                tenant.getId(),
                trialPlan.getId()
        );
    }
}
