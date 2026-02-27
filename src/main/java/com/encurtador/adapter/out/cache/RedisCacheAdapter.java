package com.encurtador.adapter.out.cache;

import com.encurtador.application.port.out.CachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Adapter de cache — implementa CachePort usando Redis.
 * Armazena o mapeamento codigo → urlOriginal com TTL de 24 horas.
 * Ativo apenas no perfil "docker" (Redis disponível).
 */
@Component
@Profile("docker")
public class RedisCacheAdapter implements CachePort {

    private static final String PREFIX = "url:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public RedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void armazenar(String codigo, String urlOriginal) {
        redisTemplate.opsForValue().set(chave(codigo), urlOriginal, TTL);
    }

    @Override
    public Optional<String> buscar(String codigo) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(chave(codigo)));
    }

    @Override
    public void remover(String codigo) {
        redisTemplate.delete(chave(codigo));
    }

    private String chave(String codigo) {
        return PREFIX + codigo;
    }
}
