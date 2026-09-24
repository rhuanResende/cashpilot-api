package com.desenvolvimento.logica.cashpilot_api.shared.exception;

public class InvalidEmailVerificationTokenException extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super("O link de confirmação é inválido ou expirou.");
    }
}
