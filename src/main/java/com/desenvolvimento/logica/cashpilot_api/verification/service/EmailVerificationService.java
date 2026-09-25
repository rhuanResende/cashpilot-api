package com.desenvolvimento.logica.cashpilot_api.verification.service;

import com.desenvolvimento.logica.cashpilot_api.shared.exception.InvalidEmailVerificationTokenException;
import com.desenvolvimento.logica.cashpilot_api.user.model.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.config.VerificationTokenGenerator;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;
import com.desenvolvimento.logica.cashpilot_api.verification.model.EmailVerificationToken;
import com.desenvolvimento.logica.cashpilot_api.verification.repository.EmailVerificationTokenRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Validated
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final Clock clock;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            VerificationTokenGenerator tokenGenerator,
            Clock clock) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    @Transactional
    public IssuedEmailVerification issue(@NotNull UUID userId) {
        var user = userRepository.findById(userId)
                .filter(candidate ->
                        candidate.getStatus() == UserStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário indisponível para confirmação de e-mail."
                ));

        if (user.getEmailVerifiedAt() != null) {
            throw new IllegalStateException(
                    "O e-mail do usuário já está confirmado."
            );
        }

        Instant expiresAt = Instant.now(clock).plus(24, ChronoUnit.HOURS);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenGenerator.hash(rawToken);

        tokenRepository.save(
                new EmailVerificationToken(user, tokenHash, expiresAt)
        );

        return new IssuedEmailVerification(
                user.getEmail(),
                rawToken,
                expiresAt
        );
    }

    @Transactional
    public void confirm(String rawToken) {
        String tokenHash;

        try {
            tokenHash = tokenGenerator.hash(rawToken);
        } catch (IllegalArgumentException exception) {
            throw new InvalidEmailVerificationTokenException();
        }

        var token = tokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        var user = userRepository.findByIdForUpdate(token.getUser().getId())
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        // Obtém o horário depois de adquirir os bloqueios.
        Instant now = Instant.now(clock);

        if (!token.isUsableAt(now)
                || user.getStatus() != UserStatus.ACTIVE
                || user.getEmailVerifiedAt() != null
                || !token.getEmail().equals(user.getEmail())) {

            throw new InvalidEmailVerificationTokenException();
        }

        token.consume(now);
        user.markEmailVerified(now);
    }
}
