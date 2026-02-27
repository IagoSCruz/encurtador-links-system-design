package com.encurtador.adapter.out.persistence;

import com.encurtador.application.port.out.UrlRepositoryPort;
import com.encurtador.domain.model.UrlEncurtada;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter de persistência — implementa o port de saída UrlRepositoryPort.
 * Faz a tradução entre o domínio (UrlEncurtada record) e a entidade JPA
 * (UrlEntity).
 */
@Component
public class UrlJpaAdapter implements UrlRepositoryPort {

    private final UrlJpaRepository jpaRepository;

    public UrlJpaAdapter(UrlJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UrlEncurtada salvar(UrlEncurtada urlEncurtada) {
        UrlEntity entity = toEntity(urlEncurtada);
        UrlEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UrlEncurtada> buscarPorCodigo(String codigo) {
        return jpaRepository.findByCodigo(codigo).map(this::toDomain);
    }

    // --- Mapeamentos manuais (domínio ↔ entidade JPA) ---

    private UrlEntity toEntity(UrlEncurtada domain) {
        return new UrlEntity(
                domain.codigo(),
                domain.urlOriginal(),
                domain.criadaEm(),
                domain.expiraEm());
    }

    private UrlEncurtada toDomain(UrlEntity entity) {
        return new UrlEncurtada(
                entity.getId(),
                entity.getCodigo(),
                entity.getUrlOriginal(),
                entity.getCriadaEm(),
                entity.getExpiraEm());
    }
}
