package com.desenvolvimento.logica.cashpilot_api.shared.exception;

public class EmailAlreadyRegisteredException extends RuntimeException  {

    public EmailAlreadyRegisteredException() {
        super("Não foi possível cadastrar com o e-mail informado.");
    }

}
