package com.desenvolvimento.logica.cashpilot_api.shared.exception;

public class LoginRateLimitUnavailableException extends RuntimeException {
    public LoginRateLimitUnavailableException() {
        super("A proteção de tentativas está indisponível");
    }

    public LoginRateLimitUnavailableException(Throwable cause) {
        super("A proteção de tentativas está indisponível", cause);
    }
}
