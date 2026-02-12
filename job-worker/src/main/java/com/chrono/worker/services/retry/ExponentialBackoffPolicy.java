package com.chrono.worker.services.retry;

import org.springframework.stereotype.Component;

@Component
public class ExponentialBackoffPolicy implements RetryPolicy {

    private static final long MAX_DELAY_MS = 300_000; // 5 minutes cap

    @Override
    public boolean isRetryable(Exception ex) {
        return !(ex instanceof IllegalArgumentException);
    }

    @Override
    public long nextDelay(int retryCount) {
        long delay = (long) Math.pow(2, retryCount) * 1000;
        return Math.min(delay, MAX_DELAY_MS);
    }
    
}
