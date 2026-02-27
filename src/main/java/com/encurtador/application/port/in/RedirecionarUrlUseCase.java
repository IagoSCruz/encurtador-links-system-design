package com.encurtador.application.port.in;

import com.encurtador.domain.model.UrlEncurtada;

/**
 * Port de entrada (use case): buscar a URL original a partir de um código.
 * Implementado por RedirecionarUrlService.
 */
public interface RedirecionarUrlUseCase {
    UrlEncurtada buscarPorCodigo(String codigo);
}
