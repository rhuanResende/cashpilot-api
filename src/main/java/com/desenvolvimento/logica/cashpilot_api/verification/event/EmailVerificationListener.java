package com.desenvolvimento.logica.cashpilot_api.verification.event;

import com.desenvolvimento.logica.cashpilot_api.mail.service.EmailVerificationMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
public class EmailVerificationListener {

    private static final Logger log =
            LoggerFactory.getLogger(EmailVerificationListener.class);

    private final EmailVerificationMailer mailer;

    public EmailVerificationListener(EmailVerificationMailer mailer) {
        this.mailer = mailer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerificationRequested(
            EmailVerificationRequestedEvent event
    ) {
        try {
            mailer.send(event.verification());
        } catch (MailException exception) {
            log.error(
                    "Falha no envio do e-mail de confirmação após o cadastro. Tipo: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
