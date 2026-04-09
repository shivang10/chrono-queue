package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class JobExecutionException extends ChronoQueueException {
    private final boolean retryable;

    public JobExecutionException(boolean retryable, String message) {
        super(ErrorCode.JOB_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, message);
        this.retryable = retryable;
    }

    public JobExecutionException(boolean retryable, String message, Throwable cause) {
        super(ErrorCode.JOB_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
