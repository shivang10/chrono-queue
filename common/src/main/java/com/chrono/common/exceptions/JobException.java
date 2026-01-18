package com.chrono.common.exceptions;

public class JobException extends RuntimeException {
    private final String jobId;

    public JobException(String jobId, String message) {
        super(message);
        this.jobId = jobId;
    }

    public JobException(String jobId, String message, Throwable cause) {
        super(message, cause);
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

}
