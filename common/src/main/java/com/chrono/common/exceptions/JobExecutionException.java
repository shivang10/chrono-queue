package com.chrono.common.exceptions;

public class JobExecutionException extends RuntimeException {
    private final boolean retryable;

    public JobExecutionException(boolean retryable, String message) {
        super(message);
        this.retryable = retryable;
    }

    public JobExecutionException(boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
