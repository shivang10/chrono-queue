package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;

public class JobPublishException extends InfrastructureException {

    public JobPublishException(String message) {
        super(ErrorCode.JOB_PUBLISH_FAILED, message);
    }

    public JobPublishException(String message, Throwable cause) {
        super(ErrorCode.JOB_PUBLISH_FAILED, message, cause);
    }
}