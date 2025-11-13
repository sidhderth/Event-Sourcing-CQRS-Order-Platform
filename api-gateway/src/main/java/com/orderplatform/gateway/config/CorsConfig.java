package com.orderplatform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for browser clients
 * Allows cross-origin requests from configured origins
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${cors.max-age}")
    private long maxAge;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Parse allowed origins from comma-separated string
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        corsConfig.setAllowedOrigins(origins);
        
        // Parse allowed methods from comma-separated string
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        corsConfig.setAllowedMethods(methods);
        
        // Configure allowed headers
        if ("*".equals(allowedHeaders)) {
            corsConfig.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            corsConfig.setAllowedHeaders(headers);
        }
        
        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(allowCredentials);
        
        // Set max age for preflight requests cache
        corsConfig.setMaxAge(maxAge);
        
        // Expose headers that clients can access
        corsConfig.addExposedHeader("X-Correlation-ID");
        corsConfig.addExposedHeader("X-RateLimit-Limit");
        corsConfig.addExposedHeader("X-RateLimit-Remaining");
        corsConfig.addExposedHeader("X-RateLimit-Reset");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
