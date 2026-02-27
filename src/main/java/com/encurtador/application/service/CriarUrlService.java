package com.encurtador.application.service;

import com.encurtador.application.port.in.CriarUrlUseCase;
import com.encurtador.application.port.out.CachePort;
import com.encurtador.application.port.out.UrlRepositoryPort;
import com.encurtador.domain.exception.UrlInvalidaException;
import com.encurtador.domain.model.UrlEncurtada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Caso de uso: criar uma URL encurtada.
 * Injeção via construtor (sem @Autowired em fields).
 */
@Service
public class CriarUrlService implements CriarUrlUseCase {

    private static final int TTL_HORAS = 24;

    private final UrlRepositoryPort repositoryPort;
    private final CachePort cachePort;
    private final GeradorCodigoBase62 geradorCodigo;

    public CriarUrlService(UrlRepositoryPort repositoryPort,
            CachePort cachePort,
            GeradorCodigoBase62 geradorCodigo) {
        this.repositoryPort = repositoryPort;
        this.cachePort = cachePort;
        this.geradorCodigo = geradorCodigo;
    }

    @Override
    @Transactional // escrita → vai para o PRIMARY
    public UrlEncurtada criar(String urlOriginal) {
        validarUrl(urlOriginal);

        String codigo = geradorCodigo.gerar();
        LocalDateTime expiraEm = LocalDateTime.now().plusHours(TTL_HORAS);

        UrlEncurtada urlEncurtada = UrlEncurtada.nova(codigo, urlOriginal, expiraEm);
        UrlEncurtada salva = repositoryPort.salvar(urlEncurtada);

        cachePort.armazenar(codigo, urlOriginal);

        return salva;
    }

    private void validarUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new UrlInvalidaException("URL não pode ser vazia");
        }
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                throw new UrlInvalidaException("apenas http e https são aceitos");
            }
        } catch (IllegalArgumentException e) {
            throw new UrlInvalidaException("formato inválido: " + url);
        }
    }
}
