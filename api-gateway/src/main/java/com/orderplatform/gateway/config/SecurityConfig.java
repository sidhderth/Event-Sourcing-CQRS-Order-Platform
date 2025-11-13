package com.orderplatform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.beans.factory.annotation.Value;

/**
 * Security configuration for API Gateway
 * Configures OAuth2 resource server with JWT validation and RBAC
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                
                // Command endpoints - require admin or ops role
                .pathMatchers(HttpMethod.POST, "/api/v1/orders/**").hasAnyRole("admin", "ops")
                .pathMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasAnyRole("admin", "ops")
                .pathMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasAnyRole("admin", "ops")
                
                // Query endpoints - require any authenticated role
                .pathMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAnyRole("admin", "ops", "analyst")
                
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    /**
     * Converts JWT claims to Spring Security authorities
     * Extracts roles from Keycloak's realm_access.roles claim
     */
    @Bean
    public KeycloakJwtAuthenticationConverter jwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }
}
