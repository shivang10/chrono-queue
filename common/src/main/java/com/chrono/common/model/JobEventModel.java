package com.chrono.common.model;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventModel {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private String jobId;
    private JobType jobType;

    private LocalDateTime createdAt;

    private Map<String, Object> payload;

    private int retryCount = 0;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private int failUntilAttempt = 0;

    private JobStatus status;

    private long executeAt;

    public static JobEventModel create(JobType jobType, Map<String, Object> payload) {
        return JobEventModel.builderInternal(jobType, payload);
    }

    private static JobEventModel builderInternal(JobType jobType, Map<String, Object> payload) {
        JobEventModel model = new JobEventModel();
        model.jobId = UUID.randomUUID().toString();
        model.jobType = jobType;
        model.payload = payload;
        model.createdAt = LocalDateTime.now();
        model.retryCount = 0;
        model.maxRetries = DEFAULT_MAX_RETRIES;
        model.failUntilAttempt = 0;
        model.status = JobStatus.PENDING;
        model.executeAt = System.currentTimeMillis();
        return model;
    }
}
