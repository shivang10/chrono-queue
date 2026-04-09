package com.chrono.common.exceptions;

import com.chrono.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class JobException extends ChronoQueueException {
    private final String jobId;

    public JobException(String jobId, String message) {
        super(ErrorCode.JOB_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, message);
        this.jobId = jobId;
    }

    public JobException(String jobId, String message, Throwable cause) {
        super(ErrorCode.JOB_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

}
