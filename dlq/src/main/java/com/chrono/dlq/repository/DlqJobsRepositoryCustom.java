package com.chrono.dlq.repository;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.DlqJobDocumentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface DlqJobsRepositoryCustom {

    Page<DlqJobDocumentModel> searchDqlJobs(
            JobType jobType,
            JobStatus status,
            Integer retryCount,
            Integer maxRetries,
            Instant createdAt,
            Pageable pageable);
}
