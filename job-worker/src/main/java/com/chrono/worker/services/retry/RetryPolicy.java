package com.chrono.worker.services.retry;

public interface RetryPolicy {
    boolean isRetryable(Exception ex);

    long nextDelay(int retryCount);
}
