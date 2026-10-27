package com.alquilereventos.alquiler.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Segunda capa de seguridad (la primera es el API Gateway): ms-alquiler valida
 * por su cuenta firma, expiracion e issuer del JWT emitido por ms-seguridad y
 * autoriza por rol, aunque alguien llame directo al puerto 8081.
 *
 * <p>Politica: seguridad por URL (aca) + seguridad por metodo
 * ({@code @PreAuthorize} en los controllers).</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class AlquilerSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt.secret}") String secret,
                          @Value("${app.security.jwt.issuer}") String issuer,
                          @Value("${app.security.jwt.algorithm:HS256}") String algorithm) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SECURITY_JWT_SECRET es obligatorio");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("SECURITY_JWT_SECRET debe estar codificado en Base64");
        }
        MacAlgorithm mac = macAlgorithm(algorithm);
        // "HS256" -> "HmacSHA256", "HS384" -> "HmacSHA384", "HS512" -> "HmacSHA512"
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA" + mac.getName().substring(2));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(mac).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private MacAlgorithm macAlgorithm(String algorithm) {
        return switch (algorithm == null ? "HS256" : algorithm.trim().toUpperCase()) {
            case "HS256" -> MacAlgorithm.HS256;
            case "HS384" -> MacAlgorithm.HS384;
            case "HS512" -> MacAlgorithm.HS512;
            default -> throw new IllegalStateException(
                    "SECURITY_JWT_ALGORITHM no soportado (use HS256, HS384 o HS512): " + algorithm);
        };
    }

    /** Convierte el claim "roles" del JWT en autoridades ROLE_ADMIN, ROLE_ENCARGADO_DEPOSITO, etc. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter,
                                            SecurityErrorHandler securityErrorHandler) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/equipos/**")
                        .hasAnyRole("ADMIN", "ENCARGADO_DEPOSITO", "ENCARGADO_COMPRAS")
                        .requestMatchers("/clientes/**", "/ordenes-alquiler/**")
                        .hasAnyRole("ADMIN", "ENCARGADO_DEPOSITO")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }
}
