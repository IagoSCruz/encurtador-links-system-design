package com.encurtador;

import com.encurtador.application.port.out.CachePort;
import com.encurtador.application.port.out.UrlRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Teste de contexto: verifica que o contexto Spring sobe sem erros.
 * Usa @MockitoBean para simular os ports de infraestrutura (banco + cache),
 * evitando a necessidade de banco ou Redis rodando nos testes.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class EncurtadorApplicationTests {

    // Mocks dos ports de saída — evita que o Spring tente criar adapters reais
    @MockitoBean
    UrlRepositoryPort urlRepositoryPort;

    @MockitoBean
    CachePort cachePort;

    @Test
    void contextLoads() {
    }

}
