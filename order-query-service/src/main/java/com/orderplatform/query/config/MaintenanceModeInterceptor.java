package com.orderplatform.query.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    @Value("${maintenance.mode.enabled}")
    private boolean maintenanceModeEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        
        // Allow health checks even in maintenance mode
        if (requestPath.startsWith("/actuator/health")) {
            return true;
        }

        // Block query endpoints during maintenance mode
        if (maintenanceModeEnabled && requestPath.startsWith("/api/v1/orders")) {
            log.warn("Request blocked due to maintenance mode: {}", requestPath);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Service Unavailable\", " +
                "\"message\": \"The service is currently in maintenance mode for event replay. Please try again later.\", " +
                "\"status\": 503}"
            );
            return false;
        }

        return true;
    }
}
