package com.desenvolvimento.logica.cashpilot_api.verification.controller;

import com.desenvolvimento.logica.cashpilot_api.verification.dto.ConfirmEmailRequest;
import com.desenvolvimento.logica.cashpilot_api.verification.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService
    ) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<Void> confirmEmail(
            @Valid @RequestBody ConfirmEmailRequest request
    ) {
        emailVerificationService.confirm(request.token());

        return ResponseEntity.noContent().build();
    }
}
