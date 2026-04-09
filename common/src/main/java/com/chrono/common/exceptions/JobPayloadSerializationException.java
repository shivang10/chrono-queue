package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;

public class JobPayloadSerializationException extends InfrastructureException {

    public JobPayloadSerializationException(String message, Throwable cause) {
        super(ErrorCode.SERIALIZATION_FAILED, message, cause);
    }
}