package com.encurtador.infrastructure.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuração do DataSource roteador.
 * Ativo apenas no perfil "docker" — em "dev" o H2 é usado diretamente.
 *
 * Monta um RoutingDataSource com:
 * - 1 datasource PRIMARY (para escrita)
 * - 3 datasources REPLICA (para leitura, round-robin)
 */
@Configuration
@Profile("docker")
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.replica1.url}")
    private String replica1Url;

    @Value("${spring.datasource.replica2.url}")
    private String replica2Url;

    @Value("${spring.datasource.replica3.url}")
    private String replica3Url;

    @Value("${spring.datasource.replica1.username}")
    private String replicaUsername;

    @Value("${spring.datasource.replica1.password}")
    private String replicaPassword;

    @Bean
    public DataSource dataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();

        // PRIMARY
        HikariDataSource primary = buildDataSource(primaryUrl, username, password);
        targetDataSources.put(DataSourceType.PRIMARY.name(), primary);

        // RÉPLICAS
        String replica1Key = "REPLICA_1";
        String replica2Key = "REPLICA_2";
        String replica3Key = "REPLICA_3";
        targetDataSources.put(replica1Key, buildDataSource(replica1Url, replicaUsername, replicaPassword));
        targetDataSources.put(replica2Key, buildDataSource(replica2Url, replicaUsername, replicaPassword));
        targetDataSources.put(replica3Key, buildDataSource(replica3Url, replicaUsername, replicaPassword));

        List<String> replicaKeys = List.of(replica1Key, replica2Key, replica3Key);
        RoutingDataSource routingDataSource = new RoutingDataSource(replicaKeys);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primary);

        return routingDataSource;
    }

    private HikariDataSource buildDataSource(String url, String user, String pass) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(30_000);
        return ds;
    }
}
