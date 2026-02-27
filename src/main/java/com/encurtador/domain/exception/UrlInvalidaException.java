package com.encurtador.domain.exception;

public class UrlInvalidaException extends RuntimeException {

    public UrlInvalidaException(String motivo) {
        super("URL inválida: " + motivo);
    }
}
