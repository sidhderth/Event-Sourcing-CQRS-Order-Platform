package com.orderplatform.command.api;

import com.orderplatform.domain.InvalidOrderStateException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        
        problemDetail.setType(URI.create("https://api.orderplatform.com/problems/validation-error"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        // Extract field violations
        Map<String, String> violations = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.put(error.getField(), error.getDefaultMessage());
        }
        problemDetail.setProperty("violations", violations);

        // Add trace ID if available
        addTraceId(problemDetail);

        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint violation occurred"
        );
        
        problemDetail.setType(URI.create("https://api.orderplatform.com/problems/validation-error"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        // Extract violations
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
        problemDetail.setProperty("violations", violations);

        addTraceId(problemDetail);

        return problemDetail;
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ProblemDetail handleInvalidOrderStateException(InvalidOrderStateException ex, WebRequest request) {
        log.warn("Invalid order state: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        
        problemDetail.setType(URI.create("https://api.orderplatform.com/problems/invalid-order-state"));
        problemDetail.setTitle("Invalid Order State");
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        addTraceId(problemDetail);

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        
        problemDetail.setType(URI.create("https://api.orderplatform.com/problems/bad-request"));
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        addTraceId(problemDetail);

        return problemDetail;
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ProblemDetail handleOptimisticLockException(OptimisticLockException ex, WebRequest request) {
        log.warn("Optimistic lock exception: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource was modified by another request. Please retry with the latest version."
        );
        
        problemDetail.setType(URI.create("https://api.orderplatform.com/problems/concurrency-conflict"));
        problemDetail.setTitle("Concurrent Modification");
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        addTraceId(problemDetail);

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        
        problemDetail.setType(URI.create("https://api.orderplatform.com/problems/internal-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        addTraceId(problemDetail);

        return problemDetail;
    }

    private void addTraceId(ProblemDetail problemDetail) {
        // In a real implementation, this would extract the trace ID from MDC or OpenTelemetry context
        // For now, we'll add a placeholder
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId != null) {
            problemDetail.setProperty("traceId", traceId);
        }
    }
}
