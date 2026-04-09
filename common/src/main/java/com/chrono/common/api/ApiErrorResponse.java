package com.chrono.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        String traceId,
        String errorCode,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors) {
}