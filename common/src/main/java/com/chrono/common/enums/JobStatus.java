package com.chrono.common.enums;

public enum JobStatus {
    PENDING("Job is pending execution"),
    SCHEDULED("Job is scheduled for future execution"),
    RUNNING("Job is currently running"),
    COMPLETED("Job has completed successfully"),
    FAILED("Job has failed"),
    CANCELLED("Job has been cancelled"),
    RETRYING("Job is being retried after failure");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}