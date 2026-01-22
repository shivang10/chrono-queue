package com.chrono.common.constants;

import com.chrono.common.enums.JobType;

public final class KafkaTopics {

    // Job Status topics
    public static final String JOB_EVENTS = "job-events";
    public static final String JOB_RESULTS = "job-results";
    public static final String JOB_DLQ = "job-dlq";
    public static final String JOB_SCHEDULED = "job-scheduled";

    // Job Events Type Topics
    public static final String WEBHOOK_JOBS = "webhook-jobs";
    public static final String EMAIL_JOBS = "email-jobs";
    public static final String PAYMENT_JOBS = "payment-processing-jobs";
    public static final String ORDER_CANCELLATION_JOBS = "order-cancellation-jobs";

    // Job Retry Topics
    public static final String DELAY_5_SECOND = "delay-5-second";
    public static final String DELAY_30_SECOND = "delay-30-second";
    public static final String DELAY_1_MINUTE = "delay-1-minute";
    public static final String DELAY_2_MINUTE = "delay-2-minute";

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
}