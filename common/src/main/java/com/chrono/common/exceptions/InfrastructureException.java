package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class InfrastructureException extends ChronoQueueException {

    public InfrastructureException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public InfrastructureException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}