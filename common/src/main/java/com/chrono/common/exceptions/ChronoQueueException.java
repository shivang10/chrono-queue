package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class ChronoQueueException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ChronoQueueException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ChronoQueueException(ErrorCode errorCode, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}