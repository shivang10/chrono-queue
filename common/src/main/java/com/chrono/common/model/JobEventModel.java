package com.chrono.common.model;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.payload.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventModel {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int CURRENT_EVENT_VERSION = 1;

    private String jobId;

    private JobType jobType;

    private int eventVersion = CURRENT_EVENT_VERSION;

    private Instant createdAt;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "jobType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = WebhookJobPayloadModel.class, name = "WEBHOOK"),
            @JsonSubTypes.Type(value = EmailJobPayloadModel.class, name = "EMAIL"),
            @JsonSubTypes.Type(value = PaymentProcessingJobPayloadModel.class, name = "PAYMENT_PROCESSING"),
            @JsonSubTypes.Type(value = OrderCancellationJobPayloadModel.class, name = "ORDER_CANCELLATION")
    })
    private JobPayloadModel payload;

    private int retryCount = 0;
    private int maxRetries = DEFAULT_MAX_RETRIES;

    private JobStatus status;

    private Instant executeAt;

    public static JobEventModel create(JobType jobType, JobPayloadModel payload) {
        return JobEventModel.builderInternal(jobType, payload);
    }

    private static JobEventModel builderInternal(JobType jobType, JobPayloadModel payload) {
        JobEventModel model = new JobEventModel();
        model.jobId = UUID.randomUUID().toString();
        model.jobType = jobType;
        model.eventVersion = CURRENT_EVENT_VERSION;
        model.payload = payload;
        model.createdAt = Instant.now();
        model.retryCount = 0;
        model.maxRetries = DEFAULT_MAX_RETRIES;
        model.status = JobStatus.PENDING;
        model.executeAt = Instant.now();
        return model;
    }
}
