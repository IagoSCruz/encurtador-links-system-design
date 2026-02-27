package com.encurtador.domain.model;

import java.time.LocalDateTime;

/**
 * Entidade de domínio — imutável por design (record Java).
 * Não possui nenhuma dependência de infraestrutura (@Entity, etc).
 */
public record UrlEncurtada(
        Long id,
        String codigo,
        String urlOriginal,
        LocalDateTime criadaEm,
        LocalDateTime expiraEm) {
    /**
     * Factory method para criar uma nova URL encurtada (sem ID — ainda não
     * persistida).
     */
    public static UrlEncurtada nova(String codigo, String urlOriginal, LocalDateTime expiraEm) {
        return new UrlEncurtada(null, codigo, urlOriginal, LocalDateTime.now(), expiraEm);
    }

    /**
     * Verifica se a URL encurtada já expirou.
     */
    public boolean expirou() {
        return expiraEm != null && LocalDateTime.now().isAfter(expiraEm);
    }
}
