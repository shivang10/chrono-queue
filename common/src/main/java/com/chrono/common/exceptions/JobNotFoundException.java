package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class JobNotFoundException extends ChronoQueueException {
    public JobNotFoundException(String jobId) {
        super(ErrorCode.JOB_NOT_FOUND, HttpStatus.NOT_FOUND, "Job not found: " + jobId);
    }

}
