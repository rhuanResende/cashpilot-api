package com.desenvolvimento.logica.cashpilot_api.auth.service;

import com.desenvolvimento.logica.cashpilot_api.auth.config.AuthenticationPolicyProperties;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.AuthenticationRequestContext;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.PasswordAuthenticationResult;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventOutcome;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventType;
import com.desenvolvimento.logica.cashpilot_api.auth.repository.UserAuthSecurityRepository;
import com.desenvolvimento.logica.cashpilot_api.user.model.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Validated
public class PasswordAuthenticationService {

    private final UserRepository userRepository;
    private final UserAuthSecurityRepository securityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationPolicyProperties policy;
    private final AuthAuditService auditService;
    private final Clock clock;
    private final String dummyPasswordHash;

    public PasswordAuthenticationService(
            UserRepository userRepository,
            UserAuthSecurityRepository securityRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationPolicyProperties policy,
            AuthAuditService auditService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.securityRepository = securityRepository;
        this.passwordEncoder = passwordEncoder;
        this.policy = policy;
        this.auditService = auditService;
        this.clock = clock;

        this.dummyPasswordHash =
                passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public PasswordAuthenticationResult authenticate(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password,
            @NotNull AuthenticationRequestContext context
    ) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);

        var userId = userRepository.findIdByEmail(normalizedEmail);

        if (userId.isEmpty()) {
            return denyWithoutPasswordCheck(
                    password, null, "INVALID_CREDENTIALS", context
            );
        }

        var lockedUser = userRepository.findByIdForUpdate(userId.get());

        if (lockedUser.isEmpty()) {
            return denyWithoutPasswordCheck(
                    password, null, "INVALID_CREDENTIALS", context
            );
        }

        var user = lockedUser.get();

        if (!user.getEmail().equals(normalizedEmail)) {
            return denyWithoutPasswordCheck(
                    password, null, "INVALID_CREDENTIALS", context
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            return denyWithoutPasswordCheck(
                    password, user.getId(), "ACCOUNT_BLOCKED", context
            );
        }

        var security = securityRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Registro de segurança do usuário não encontrado"
                ));

        if (security.isPasswordLockedAt(Instant.now(clock))) {
            return denyWithoutPasswordCheck(
                    password,
                    user.getId(),
                    "TEMPORARILY_LOCKED",
                    context
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(password, user.getPasswordHash());

        if (!passwordMatches) {
            var passwordPolicy = policy.password();

            boolean lockApplied = security.registerPasswordFailure(
                    Instant.now(clock),
                    passwordPolicy.maxFailedAttempts(),
                    passwordPolicy.failureWindow(),
                    passwordPolicy.lockDuration()
            );

            record(
                    AuthEventType.PASSWORD_CHECK,
                    AuthEventOutcome.FAILURE,
                    "INVALID_CREDENTIALS",
                    user.getId(),
                    context
            );

            if (lockApplied) {
                record(
                        AuthEventType.TEMPORARY_LOCK_APPLIED,
                        AuthEventOutcome.SUCCESS,
                        "PASSWORD_ATTEMPT_LIMIT",
                        user.getId(),
                        context
                );
            }

            return PasswordAuthenticationResult.denied();
        }

        security.resetPasswordAttempts();

        record(
                AuthEventType.PASSWORD_CHECK,
                AuthEventOutcome.SUCCESS,
                null,
                user.getId(),
                context
        );

        if (user.getEmailVerifiedAt() == null) {
            record(
                    AuthEventType.ACCESS_DENIED,
                    AuthEventOutcome.DENIED,
                    "EMAIL_NOT_VERIFIED",
                    user.getId(),
                    context
            );

            return PasswordAuthenticationResult.denied();
        }

        return PasswordAuthenticationResult.verified(user.getId());
    }

    private PasswordAuthenticationResult denyWithoutPasswordCheck(
            String password,
            UUID userId,
            String reasonCode,
            AuthenticationRequestContext context
    ) {
        // O resultado é descartado: este caminho sempre nega o acesso.
        passwordEncoder.matches(password, dummyPasswordHash);

        record(
                AuthEventType.PASSWORD_CHECK,
                "INVALID_CREDENTIALS".equals(reasonCode)
                        ? AuthEventOutcome.FAILURE
                        : AuthEventOutcome.DENIED,
                reasonCode,
                userId,
                context
        );

        return PasswordAuthenticationResult.denied();
    }

    private void record(
            AuthEventType eventType,
            AuthEventOutcome outcome,
            String reasonCode,
            UUID userId,
            AuthenticationRequestContext context
    ) {
        auditService.record(
                eventType,
                outcome,
                reasonCode,
                userId,
                null,
                null,
                context.sourceIp(),
                context.userAgent(),
                context.requestId()
        );
    }

}
