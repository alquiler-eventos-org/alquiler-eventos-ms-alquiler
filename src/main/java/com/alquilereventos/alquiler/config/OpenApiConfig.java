package com.alquilereventos.alquiler.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacion OpenAPI del microservicio.
 * El "server" apunta al API Gateway (no al microservicio) para que el boton
 * "Try it out" del Swagger del gateway pase siempre por el gateway.
 * Declara el esquema "bearerAuth" (JWT) para el boton "Authorize" de Swagger.
 * Todo se configura en application.yml (app.openapi.*).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${app.openapi.title}") String title,
            @Value("${app.openapi.description}") String description,
            @Value("${app.openapi.version}") String version,
            @Value("${app.openapi.server-url}") String serverUrl) {
        return new OpenAPI()
                .info(new Info().title(title).description(description).version(version))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .servers(List.of(new Server().url(serverUrl).description("API Gateway")));
    }
}
