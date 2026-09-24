package com.desenvolvimento.logica.cashpilot_api.shared.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Usuário não encontrado.");
    }
}
