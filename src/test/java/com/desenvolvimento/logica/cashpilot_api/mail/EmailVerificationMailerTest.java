package com.desenvolvimento.logica.cashpilot_api.mail;

import com.desenvolvimento.logica.cashpilot_api.mail.service.EmailVerificationMailer;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmailVerificationMailerTest {

    @Test
    void shouldSendEmailWithVerificationLink() {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        EmailVerificationMailer mailer = new EmailVerificationMailer(
                mailSender,
                "no-reply@cashpilot.test",
                "http://localhost:5173/confirm-email"
        );

        String token = "a".repeat(43);

        IssuedEmailVerification verification = new IssuedEmailVerification(
                "usuario@example.com",
                token,
                Instant.parse("2026-10-01T12:00:00Z")
        );

        mailer.send(verification);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertAll(
                () -> assertEquals(
                        "no-reply@cashpilot.test",
                        message.getFrom()
                ),
                () -> assertArrayEquals(
                        new String[]{"usuario@example.com"},
                        message.getTo()
                ),
                () -> assertEquals(
                        "CashPilot — confirme seu e-mail",
                        message.getSubject()
                ),
                () -> {
                    assertNotNull(message.getText());
                    assertTrue(message.getText().contains(
                            "http://localhost:5173/confirm-email#token=" + token
                    ));
                }
        );
    }
}
