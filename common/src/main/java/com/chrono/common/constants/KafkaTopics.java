package com.chrono.common.constants;

import com.chrono.common.enums.JobType;

import java.util.Set;

public final class KafkaTopics {

    // Job Status topics
    public static final String JOB_RESULTS = "job-results";
    public static final String JOB_DLQ = "job-dlq";

    // Job Events Type Topics
    public static final String WEBHOOK_JOBS = "webhook-jobs";
    public static final String EMAIL_JOBS = "email-jobs";
    public static final String PAYMENT_JOBS = "payment-processing-jobs";
    public static final String ORDER_CANCELLATION_JOBS = "order-cancellation-jobs";

    private KafkaTopics() {
        throw new AssertionError("Cannot instantiate KafkaTopics - constant class.");
    }

    public static String getTopicForJobType(JobType jobType) {
        switch (jobType) {
            case WEBHOOK:
                return WEBHOOK_JOBS;
            case EMAIL:
                return EMAIL_JOBS;
            case PAYMENT_PROCESSING:
                return PAYMENT_JOBS;
            case ORDER_CANCELLATION:
                return ORDER_CANCELLATION_JOBS;
            default:
                throw new IllegalArgumentException("Unsupported JobType: " + jobType);
        }
    }

    public static Set<String> getJobTypeTopics() {
        return Set.of(
                WEBHOOK_JOBS,
                EMAIL_JOBS,
                PAYMENT_JOBS,
                ORDER_CANCELLATION_JOBS);
    }

    public static Set<String> getAllTopics() {
        return Set.of(
                JOB_RESULTS,
                JOB_DLQ,
                WEBHOOK_JOBS,
                EMAIL_JOBS,
                PAYMENT_JOBS,
                ORDER_CANCELLATION_JOBS);
    }
}