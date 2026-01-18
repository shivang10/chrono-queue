package com.chrono.common.exceptions;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String jobId) {
        super("Job not found: " + jobId);
    }

}
