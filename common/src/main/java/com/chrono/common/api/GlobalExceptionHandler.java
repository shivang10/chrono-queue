package com.chrono.common.api;

import com.chrono.common.exceptions.ChronoQueueException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice(basePackages = "com.chrono")
public class GlobalExceptionHandler {

    @ExceptionHandler(ChronoQueueException.class)
    public ResponseEntity<ApiErrorResponse> handleChronoQueueException(
            ChronoQueueException ex,
            WebRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Server-side exception [{}] - status: {}, message: {}",
                    ex.getErrorCode(), ex.getStatus().value(), ex.getMessage(), ex);
        } else {
            log.warn("Client exception [{}] - status: {}, message: {}",
                    ex.getErrorCode(), ex.getStatus().value(), ex.getMessage());
        }
        return buildResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Request validation failed - fields: {}", validationErrors);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed",
                request,
                validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(
                violation -> validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        log.warn("Request parameter validation failed - violations: {}", validationErrors);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed",
                request,
                validationErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "Malformed request body",
                request,
                null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        log.warn("Invalid request: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                ex.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            WebRequest request) {
        log.error("Unhandled exception", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred",
                request,
                null);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            WebRequest request,
            Map<String, String> validationErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                resolveTraceId(request),
                errorCode.name(),
                status.value(),
                status.getReasonPhrase(),
                message,
                resolvePath(request),
                validationErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String resolveTraceId(WebRequest request) {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }

        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }

        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }

        return UUID.randomUUID().toString();
    }

    private String resolvePath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}