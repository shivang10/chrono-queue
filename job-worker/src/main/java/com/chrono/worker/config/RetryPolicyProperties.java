package com.chrono.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "retry.policy")
public class RetryPolicyProperties {

    private int maxRetries = 3;
    private long initialDelay = 5000;
    private double multiplier = 2.0;
    private long maxDelay = 300000;

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
    }
}
