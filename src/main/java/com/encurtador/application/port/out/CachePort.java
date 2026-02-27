package com.encurtador.application.port.out;

import java.util.Optional;

/**
 * Port de saída: contrato de cache para redirecionamento.
 * Implementado por RedisCacheAdapter.
 */
public interface CachePort {
    void armazenar(String codigo, String urlOriginal);

    Optional<String> buscar(String codigo);

    void remover(String codigo);
}
