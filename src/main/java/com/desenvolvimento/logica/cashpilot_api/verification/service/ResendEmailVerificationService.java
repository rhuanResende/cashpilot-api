package com.desenvolvimento.logica.cashpilot_api.verification.service;

import com.desenvolvimento.logica.cashpilot_api.user.model.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.ResendVerificationRequest;
import com.desenvolvimento.logica.cashpilot_api.verification.event.EmailVerificationRequestedEvent;
import com.desenvolvimento.logica.cashpilot_api.verification.repository.EmailVerificationTokenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@Validated
public class ResendEmailVerificationService {

    private static final Duration RESEND_INTERVAL =
            Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ResendEmailVerificationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationService emailVerificationService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailVerificationService = emailVerificationService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void resend(
            @NotNull @Valid ResendVerificationRequest request
    ) {
        var userId = userRepository.findIdByEmail(request.email());

        if (userId.isEmpty()) {
            return;
        }

        var lockedUser = userRepository.findByIdForUpdate(userId.get());

        if (lockedUser.isEmpty()) {
            return;
        }

        var user = lockedUser.get();

        if (user.getStatus() != UserStatus.ACTIVE
                || user.getEmailVerifiedAt() != null
                || !user.getEmail().equals(request.email())) {
            return;
        }

        Instant now = Instant.now(clock);

        var lastIssuedAt =
                tokenRepository.findLastIssuedAtByUserId(user.getId());

        if (lastIssuedAt.isPresent()
                && now.isBefore(lastIssuedAt.get().plus(RESEND_INTERVAL))) {
            return;
        }

        var verification = emailVerificationService.issue(user.getId());

        eventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(verification)
        );
    }
}
