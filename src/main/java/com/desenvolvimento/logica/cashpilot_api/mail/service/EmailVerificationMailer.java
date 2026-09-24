package com.desenvolvimento.logica.cashpilot_api.mail.service;

import com.desenvolvimento.logica.cashpilot_api.verification.dto.IssuedEmailVerification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailVerificationMailer {

    private final JavaMailSender mailSender;
    private final String from;
    private final String verificationUrl;

    public EmailVerificationMailer(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.verification-url}") String verificationUrl) {

        this.mailSender = mailSender;
        this.from = from;
        this.verificationUrl = verificationUrl;
    }

    public void send(IssuedEmailVerification verification) {
        String link = UriComponentsBuilder
                .fromUriString(verificationUrl)
                .fragment("token=" + verification.token())
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(verification.email());
        message.setSubject("CashPilot — confirme seu e-mail");
        message.setText("""
                Olá!

                Confirme seu endereço de e-mail para continuar no CashPilot.

                Acesse:
                %s

                Este link é válido por 24 horas e pode ser utilizado uma única vez.

                Se você não solicitou este cadastro, ignore esta mensagem.

                Equipe CashPilot
                """.formatted(link));

        mailSender.send(message);
    }
}
