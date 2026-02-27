package com.encurtador.application.port.out;

import com.encurtador.domain.model.UrlEncurtada;

import java.util.Optional;

/**
 * Port de saída: contrato de persistência das URLs encurtadas.
 * Implementado por UrlJpaAdapter (adapter de persistência).
 */
public interface UrlRepositoryPort {
    UrlEncurtada salvar(UrlEncurtada urlEncurtada);

    Optional<UrlEncurtada> buscarPorCodigo(String codigo);
}
