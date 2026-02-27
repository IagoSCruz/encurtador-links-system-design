package com.encurtador.application.service;

import com.encurtador.application.port.in.RedirecionarUrlUseCase;
import com.encurtador.application.port.out.CachePort;
import com.encurtador.application.port.out.UrlRepositoryPort;
import com.encurtador.domain.exception.UrlNaoEncontradaException;
import com.encurtador.domain.model.UrlEncurtada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: redirecionar a partir de um código curto.
 * Estratégia: Redis primeiro → réplica PostgreSQL em cache miss.
 */
@Service
public class RedirecionarUrlService implements RedirecionarUrlUseCase {

    private final UrlRepositoryPort repositoryPort;
    private final CachePort cachePort;

    public RedirecionarUrlService(UrlRepositoryPort repositoryPort, CachePort cachePort) {
        this.repositoryPort = repositoryPort;
        this.cachePort = cachePort;
    }

    @Override
    @Transactional(readOnly = true) // leitura → vai para RÉPLICA via RoutingDataSource
    public UrlEncurtada buscarPorCodigo(String codigo) {

        // ① Tenta o cache primeiro (Redis)
        return cachePort.buscar(codigo)
                .map(urlOriginal -> urlOriginalParaDominio(codigo, urlOriginal))
                .orElseGet(() -> buscarNoBancoERepovoarCache(codigo));
    }

    private UrlEncurtada buscarNoBancoERepovoarCache(String codigo) {
        // ② Cache miss — consulta a réplica PostgreSQL
        UrlEncurtada encontrada = repositoryPort.buscarPorCodigo(codigo)
                .orElseThrow(() -> new UrlNaoEncontradaException(codigo));

        if (encontrada.expirou()) {
            throw new UrlNaoEncontradaException(codigo);
        }

        // ③ Repopula o cache para as próximas requisições
        cachePort.armazenar(codigo, encontrada.urlOriginal());

        return encontrada;
    }

    private UrlEncurtada urlOriginalParaDominio(String codigo, String urlOriginal) {
        // Constrói um objeto de domínio mínimo a partir do cache (sem dados de
        // persistência)
        return new UrlEncurtada(null, codigo, urlOriginal, null, null);
    }
}
