package com.encurtador.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA — confinada ao pacote de persistência.
 * Nunca deve ser exposta fora do adapter de persistência.
 */
@Entity
@Table(name = "url_encurtada", indexes = @Index(name = "idx_url_encurtada_codigo", columnList = "codigo", unique = true))
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "url_original", nullable = false, length = 2048)
    private String urlOriginal;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    // JPA exige construtor padrão
    protected UrlEntity() {
    }

    public UrlEntity(String codigo, String urlOriginal, LocalDateTime criadaEm, LocalDateTime expiraEm) {
        this.codigo = codigo;
        this.urlOriginal = urlOriginal;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getUrlOriginal() {
        return urlOriginal;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }
}
