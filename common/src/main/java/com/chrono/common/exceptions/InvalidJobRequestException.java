package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidJobRequestException extends ChronoQueueException {

    public InvalidJobRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    public InvalidJobRequestException(String message, Throwable cause) {
        super(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, message, cause);
    }
}