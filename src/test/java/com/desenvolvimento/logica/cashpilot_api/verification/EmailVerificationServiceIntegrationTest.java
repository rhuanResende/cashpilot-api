package com.desenvolvimento.logica.cashpilot_api.verification;

import com.desenvolvimento.logica.cashpilot_api.shared.exception.InvalidEmailVerificationTokenException;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.config.VerificationTokenGenerator;
import com.desenvolvimento.logica.cashpilot_api.verification.repository.EmailVerificationTokenRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.service.EmailVerificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class EmailVerificationServiceIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private EmailVerificationService verificationService;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private VerificationTokenGenerator tokenGenerator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private Clock clock;

    private Instant now;

    @BeforeEach
    void configureClock() {
        now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(clock.instant()).thenReturn(now);
    }

    @Test
    void shouldStoreOnlyTokenHashWith24HourExpiration() {
        User user = createUser();

        var issued = verificationService.issue(user.getId());

        entityManager.flush();
        entityManager.clear();

        var stored = tokenRepository.findByTokenHashForUpdate(
                tokenGenerator.hash(issued.token())
        ).orElseThrow();

        assertThat(stored.getTokenHash()).isNotEqualTo(issued.token());
        assertThat(stored.getTokenHash())
                .isEqualTo(tokenGenerator.hash(issued.token()));
        assertThat(stored.getEmail()).isEqualTo(user.getEmail());
        assertThat(stored.getExpiresAt())
                .isEqualTo(now.plus(24, ChronoUnit.HOURS));
        assertThat(stored.getConsumedAt()).isNull();
    }

    @Test
    void shouldConfirmEmailAndConsumeToken() {
        User user = createUser();
        UUID userId = user.getId();
        var issued = verificationService.issue(userId);

        verificationService.confirm(issued.token());

        entityManager.flush();
        entityManager.clear();

        var updatedUser = userRepository.findById(userId).orElseThrow();

        var storedToken = tokenRepository.findByTokenHashForUpdate(
                tokenGenerator.hash(issued.token())
        ).orElseThrow();

        assertThat(updatedUser.getEmailVerifiedAt()).isEqualTo(now);
        assertThat(storedToken.getConsumedAt()).isEqualTo(now);
    }

    @Test
    void shouldRejectTokenAtExpirationTime() {
        User user = createUser();
        UUID userId = user.getId();
        var issued = verificationService.issue(userId);

        entityManager.flush();
        entityManager.clear();

        when(clock.instant()).thenReturn(issued.expiresAt());

        assertThatThrownBy(() -> verificationService.confirm(issued.token()))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);

        var unchangedUser = userRepository.findById(userId).orElseThrow();

        var storedToken = tokenRepository.findByTokenHashForUpdate(
                tokenGenerator.hash(issued.token())
        ).orElseThrow();

        assertThat(unchangedUser.getEmailVerifiedAt()).isNull();
        assertThat(storedToken.getConsumedAt()).isNull();
    }

    @Test
    void shouldRejectPreviouslyConsumedToken() {
        User user = createUser();
        var issued = verificationService.issue(user.getId());

        verificationService.confirm(issued.token());

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> verificationService.confirm(issued.token()))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);
    }

    private User createUser() {
        return userRepository.saveAndFlush(new User(
                "Profissional de teste",
                "verificacao-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Uma senha longa para teste!")
        ));
    }
}
