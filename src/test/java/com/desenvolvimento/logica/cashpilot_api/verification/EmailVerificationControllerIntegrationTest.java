package com.desenvolvimento.logica.cashpilot_api.verification;

import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;
import com.desenvolvimento.logica.cashpilot_api.verification.service.EmailVerificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailVerificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void shouldConfirmValidToken() throws Exception {
        IssuedEmailVerification verification = issueToken();

        confirm(verification.token())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        entityManager.flush();
        entityManager.clear();

        User user = userRepository.findByEmail(verification.email())
                .orElseThrow();

        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void shouldRejectAlreadyUsedToken() throws Exception {
        IssuedEmailVerification verification = issueToken();

        confirm(verification.token())
                .andExpect(status().isNoContent());

        confirm(verification.token())
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        IssuedEmailVerification verification = issueToken();

        entityManager.flush();

        // Coloca o token no passado, preservando expires_at > created_at.
        jdbcTemplate.update(
                """
                UPDATE email_verification_tokens
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 days',
                    expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
                WHERE user_id = (
                    SELECT id FROM users WHERE email = ?
                )
                """,
                verification.email()
        );

        // Evita que o Hibernate reutilize as datas anteriores em memória.
        entityManager.clear();

        confirm(verification.token())
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnknownToken() throws Exception {
        // Formato válido, mas sem registro correspondente no banco.
        confirm("a".repeat(43))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/confirm-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        confirm("token-invalido")
                .andExpect(status().isBadRequest());
    }

    private IssuedEmailVerification issueToken() {
        String email = "confirm-http-" + UUID.randomUUID()
                + "@example.com";

        User user = userRepository.save(
                new User(
                        "Profissional de teste",
                        email,
                        "hash-ficticio-nao-utilizado-para-login"
                )
        );

        return emailVerificationService.issue(user.getId());
    }

    private ResultActions confirm(String token) throws Exception {
        return mockMvc.perform(
                post("/api/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s"}
                                """.formatted(token))
        );
    }
}