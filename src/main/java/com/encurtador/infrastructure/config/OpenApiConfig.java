package com.encurtador.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Encurtador de Links API")
                        .description("""
                                API REST para encurtamento de URLs.

                                **Arquitetura:** Hexagonal (Ports & Adapters)

                                **Infraestrutura:**
                                - PostgreSQL (primário + 3 réplicas com streaming replication)
                                - Redis (cache com padrão Cache-Aside)
                                - Kong API Gateway
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Encurtador Dev Team")
                                .email("dev@encurtador.com"))
                        .license(new License()
                                .name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Dev local (H2)"),
                        new Server().url("http://localhost:8000").description("Docker via Kong")));
    }
}
