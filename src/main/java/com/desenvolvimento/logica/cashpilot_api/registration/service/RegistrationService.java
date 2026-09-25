package com.desenvolvimento.logica.cashpilot_api.registration.service;

import com.desenvolvimento.logica.cashpilot_api.auth.model.UserAuthSecurity;
import com.desenvolvimento.logica.cashpilot_api.auth.repository.UserAuthSecurityRepository;
import com.desenvolvimento.logica.cashpilot_api.membership.model.MembershipRole;
import com.desenvolvimento.logica.cashpilot_api.membership.model.TenantMembership;
import com.desenvolvimento.logica.cashpilot_api.membership.repository.TenantMembershipRepository;
import com.desenvolvimento.logica.cashpilot_api.plan.model.Plan;
import com.desenvolvimento.logica.cashpilot_api.plan.repository.PlanRepository;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterRequest;
import com.desenvolvimento.logica.cashpilot_api.registration.dto.RegisterResponse;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.EmailAlreadyRegisteredException;
import com.desenvolvimento.logica.cashpilot_api.subscription.model.Subscription;
import com.desenvolvimento.logica.cashpilot_api.subscription.repository.SubscriptionRepository;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;
import com.desenvolvimento.logica.cashpilot_api.verification.event.EmailVerificationRequestedEvent;
import com.desenvolvimento.logica.cashpilot_api.verification.service.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
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
    private final UserAuthSecurityRepository authSecurityRepository;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            UserAuthSecurityRepository authSecurityRepository,
            Clock clock,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.authSecurityRepository = authSecurityRepository;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.eventPublisher = eventPublisher;
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

        authSecurityRepository.save(new UserAuthSecurity(user));

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

        IssuedEmailVerification verification =
                emailVerificationService.issue(user.getId());

        eventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(verification)
        );

        return new RegisterResponse(
                user.getId(),
                tenant.getId(),
                trialPlan.getId()
        );
    }
}
