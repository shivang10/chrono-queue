package com.chrono.common.validation;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.payload.JobPayloadModel;

public final class JobPayloadValidator {

    private JobPayloadValidator() {
    }

    public static void validateCompatibility(JobType jobType, JobPayloadModel payload) {
        if (jobType == null) {
            throw new IllegalArgumentException("jobType cannot be null");
        }

        if (payload == null) {
            throw new IllegalArgumentException("payload cannot be null");
        }

        if (payload.supportedJobType() != jobType) {
            throw new IllegalArgumentException(
                    "Payload type " + payload.getClass().getSimpleName() + " is incompatible with jobType " + jobType);
        }

        if (payload.getSchemaVersion() < 1) {
            throw new IllegalArgumentException("payload schemaVersion must be >= 1");
        }
    }
}
