package com.encurtador.application.port.in;

import com.encurtador.domain.model.UrlEncurtada;

/**
 * Port de entrada (use case): encurtar uma URL.
 * Implementado por CriarUrlService.
 */
public interface CriarUrlUseCase {
    UrlEncurtada criar(String urlOriginal);
}
