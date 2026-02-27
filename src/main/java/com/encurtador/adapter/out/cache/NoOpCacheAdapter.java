package com.encurtador.adapter.out.cache;

import com.encurtador.application.port.out.CachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter de cache no-op — ativo apenas no perfil "dev".
 * Substitui o RedisCacheAdapter quando Redis não está disponível.
 * Todas as operações são silenciadas: cache miss sempre → vai ao banco.
 */
@Component
@Profile("dev")
public class NoOpCacheAdapter implements CachePort {

    @Override
    public void armazenar(String codigo, String urlOriginal) {
        // no-op: sem cache no perfil dev
    }

    @Override
    public Optional<String> buscar(String codigo) {
        return Optional.empty(); // sempre cache miss → vai ao H2
    }

    @Override
    public void remover(String codigo) {
        // no-op
    }
}
