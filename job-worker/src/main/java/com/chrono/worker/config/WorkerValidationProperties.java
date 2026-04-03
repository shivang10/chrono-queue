package com.chrono.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "worker.validation")
public class WorkerValidationProperties {

    private double simulatedFailureRate = 0.5;
    private Long randomSeed;

    public double getSimulatedFailureRate() {
        return simulatedFailureRate;
    }

    public void setSimulatedFailureRate(double simulatedFailureRate) {
        this.simulatedFailureRate = simulatedFailureRate;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }
}
