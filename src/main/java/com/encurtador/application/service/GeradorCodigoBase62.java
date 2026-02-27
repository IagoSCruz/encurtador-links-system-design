package com.encurtador.application.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utilitário para gerar códigos curtos em Base62 (a-z, A-Z, 0-9).
 * A partir de um UUID aleatório, extrai os primeiros 8 caracteres em Base62.
 * Colisões são extremamente improváveis (~3.5 trilhões de combinações em 8
 * chars).
 */
@Component
public class GeradorCodigoBase62 {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = ALPHABET.length();
    private static final int TAMANHO_CODIGO = 8;

    public String gerar() {
        long valor = Math.abs(UUID.randomUUID().getMostSignificantBits());
        return toBase62(valor);
    }

    private String toBase62(long valor) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < TAMANHO_CODIGO) {
            sb.append(ALPHABET.charAt((int) (valor % BASE)));
            valor /= BASE;
        }
        return sb.reverse().toString();
    }
}
