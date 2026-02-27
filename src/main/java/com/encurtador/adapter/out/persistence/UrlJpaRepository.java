package com.encurtador.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA — confinado ao pacote de persistência.
 */
public interface UrlJpaRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByCodigo(String codigo);
}
