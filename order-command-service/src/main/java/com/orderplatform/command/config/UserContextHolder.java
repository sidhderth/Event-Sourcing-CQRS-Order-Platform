package com.orderplatform.command.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserContextHolder {

    /**
     * Extracts the user ID from the JWT token in the security context.
     *
     * @return Optional containing the user ID, or empty if not available
     */
    public Optional<String> getUserId() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("sub"));
    }

    /**
     * Extracts the username from the JWT token in the security context.
     *
     * @return Optional containing the username, or empty if not available
     */
    public Optional<String> getUsername() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("preferred_username"));
    }

    /**
     * Extracts the email from the JWT token in the security context.
     *
     * @return Optional containing the email, or empty if not available
     */
    public Optional<String> getEmail() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Gets the JWT token from the security context.
     *
     * @return Optional containing the JWT, or empty if not available
     */
    private Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        
        return Optional.empty();
    }
}
