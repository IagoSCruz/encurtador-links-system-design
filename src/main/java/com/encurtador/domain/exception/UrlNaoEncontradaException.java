package com.encurtador.domain.exception;

public class UrlNaoEncontradaException extends RuntimeException {

    public UrlNaoEncontradaException(String codigo) {
        super("URL não encontrada para o código: " + codigo);
    }
}
