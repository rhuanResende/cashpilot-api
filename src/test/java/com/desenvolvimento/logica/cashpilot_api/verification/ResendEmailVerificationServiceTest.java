package com.desenvolvimento.logica.cashpilot_api.verification;

import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import com.desenvolvimento.logica.cashpilot_api.user.model.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.ResendVerificationRequest;
import com.desenvolvimento.logica.cashpilot_api.verification.event.EmailVerificationRequestedEvent;
import com.desenvolvimento.logica.cashpilot_api.verification.repository.EmailVerificationTokenRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.service.EmailVerificationService;
import com.desenvolvimento.logica.cashpilot_api.verification.service.ResendEmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class ResendEmailVerificationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-09-24T12:00:00Z");

    private static final String EMAIL = "usuario@example.com";

    private final UUID userId = UUID.randomUUID();

    private final UserRepository userRepository =
            mock(UserRepository.class);

    private final EmailVerificationTokenRepository tokenRepository =
            mock(EmailVerificationTokenRepository.class);

    private final EmailVerificationService verificationService =
            mock(EmailVerificationService.class);

    private final ApplicationEventPublisher eventPublisher =
            mock(ApplicationEventPublisher.class);

    private final ResendEmailVerificationService service =
            new ResendEmailVerificationService(
                    userRepository,
                    tokenRepository,
                    verificationService,
                    eventPublisher,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void shouldResendExactlyAtSixtySeconds() {
        eligibleUser();

        when(tokenRepository.findLastIssuedAtByUserId(userId))
                .thenReturn(Optional.of(NOW.minusSeconds(60)));

        var issued = new IssuedEmailVerification(
                EMAIL,
                "a".repeat(43),
                NOW.plusSeconds(86_400)
        );

        when(verificationService.issue(userId)).thenReturn(issued);

        service.resend(request());

        verify(verificationService, times(1)).issue(userId);

        verify(eventPublisher, times(1)).publishEvent(
                new EmailVerificationRequestedEvent(issued)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 59})
    void shouldNotResendBeforeSixtySeconds(long elapsedSeconds) {
        eligibleUser();

        when(tokenRepository.findLastIssuedAtByUserId(userId))
                .thenReturn(Optional.of(NOW.minusSeconds(elapsedSeconds)));

        service.resend(request());

        verifyNoInteractions(verificationService, eventPublisher);
    }

    @Test
    void shouldNotResendForUnknownEmail() {
        when(userRepository.findIdByEmail(EMAIL))
                .thenReturn(Optional.empty());

        service.resend(request());

        verify(userRepository, never()).findByIdForUpdate(any());
        verifyNoInteractions(
                tokenRepository,
                verificationService,
                eventPublisher
        );
    }

    @Test
    void shouldNotResendForBlockedUser() {
        User user = eligibleUser();

        when(user.getStatus()).thenReturn(UserStatus.BLOCKED);

        service.resend(request());

        verifyNoInteractions(
                tokenRepository,
                verificationService,
                eventPublisher
        );
    }

    @Test
    void shouldNotResendForVerifiedUser() {
        User user = eligibleUser();

        when(user.getEmailVerifiedAt()).thenReturn(NOW.minusSeconds(120));

        service.resend(request());

        verifyNoInteractions(
                tokenRepository,
                verificationService,
                eventPublisher
        );
    }

    @Test
    void shouldIssueWhenUserHasNoPreviousToken() {
        eligibleUser();

        when(tokenRepository.findLastIssuedAtByUserId(userId))
                .thenReturn(Optional.empty());

        var issued = new IssuedEmailVerification(
                EMAIL,
                "b".repeat(43),
                NOW.plusSeconds(86_400)
        );

        when(verificationService.issue(userId)).thenReturn(issued);

        service.resend(request());

        verify(verificationService, times(1)).issue(userId);

        verify(eventPublisher, times(1)).publishEvent(
                new EmailVerificationRequestedEvent(issued)
        );
    }

    private User eligibleUser() {
        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getEmailVerifiedAt()).thenReturn(null);

        when(userRepository.findIdByEmail(EMAIL))
                .thenReturn(Optional.of(userId));

        when(userRepository.findByIdForUpdate(userId))
                .thenReturn(Optional.of(user));

        return user;
    }

    private ResendVerificationRequest request() {
        return new ResendVerificationRequest(EMAIL);
    }
}