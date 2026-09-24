package com.desenvolvimento.logica.cashpilot_api.shared.exception;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException() {
        super("Você não tem permissão para realizar esta operação.");
    }
}
