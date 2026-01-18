package com.chrono.common.constants;

public final class KafkaTopics {

    public static final String JOB_EVENTS = "job-events";
    public static final String JOB_RESULTS = "job-results";
    public static final String JOB_DLQ = "job-dlq";
    public static final String JOB_SCHEDULED = "job-scheduled";
    private KafkaTopics() {
        throw new AssertionError("Cannot instantiate KafkaTopics - constant class.");
    }
}