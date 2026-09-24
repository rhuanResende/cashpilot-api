package com.desenvolvimento.logica.cashpilot_api.verification.controller;

import com.desenvolvimento.logica.cashpilot_api.verification.dto.ResendVerificationRequest;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.ResendVerificationResponse;
import com.desenvolvimento.logica.cashpilot_api.verification.service.ResendEmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ResendEmailVerificationController {

    private final ResendEmailVerificationService resendService;

    public ResendEmailVerificationController(
            ResendEmailVerificationService resendService
    ) {
        this.resendService = resendService;
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ResendVerificationResponse> resend(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        resendService.resend(request);

        return ResponseEntity.ok(
                new ResendVerificationResponse(
                        "Se houver uma conta elegível para esse endereço, "
                                + "enviaremos as instruções de confirmação."
                )
        );
    }
}
