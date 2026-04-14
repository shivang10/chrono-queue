package com.chrono.common.model;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.payload.JobPayloadModel;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "dlq_db")
@CompoundIndex(name = "status_executeAt_idx", def = "{'status': 1, 'executeAt': 1}")
@Data
public class DlqJobDocumentModel {

    private static final int DEFAULT_MAX_RETRIES = 3;

    @Field("job_id")
    @Id
    private String jobId;

    @Field("job_type")
    @Indexed
    private JobType jobType;

    @Field("created_at")
    private Instant createdAt;

    @Field("event_version")
    private int eventVersion = 1;

    private JobPayloadModel payload;

    @Field("retry_count")
    private int retryCount = 0;

    @Field("max_retries")
    private int maxRetries = DEFAULT_MAX_RETRIES;

    @Indexed
    private JobStatus status;

    @Field("execute_at")
    private Instant executeAt;

    public DlqJobDocumentModel() {
        this.createdAt = Instant.now();
    }

}
