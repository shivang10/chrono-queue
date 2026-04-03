package com.chrono.dlq.service;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.repository.DlqJobsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
public class DlqHandlerService {
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "retryCount", "jobId", "maxRetries", "status", "jobType");

    private final DlqJobsRepository dlqJobsRepository;

    public DlqHandlerService(DlqJobsRepository dlqJobsRepository) {
        this.dlqJobsRepository = dlqJobsRepository;
    }

    public void handleDlqMessage(JobEventModel jobEvent, String topic) {
        log.info("Handling DLQ message for job: {} from topic: {}",
                jobEvent.getJobId(), topic);
    }

    public void saveFailedJob(JobEventModel jobEvent) {
        if (jobEvent == null) {
            log.error("JobEventModel is null");
            return;
        }
        dlqJobsRepository.save(jobEvent);
        log.info("Saved failed job with ID: {} to DLQ repository", jobEvent.getJobId());
    }

    public Page<JobEventModel> getDlqJobs(
            int page,
            int limit,
            JobType jobType,
            JobStatus status,
            Integer retryCount,
            Integer maxRetries,
            String createdAt,
            String sortBy,
            String sortDir) {
        log.info(
                "Fetching DLQ jobs with filters - page: {}, limit: {}, jobType: {}, status: {}, retryCount: {}, maxRetries: {}, createdAt: {}, sortBy: {}, sortDir: {}",
                page, limit, jobType, status, retryCount, maxRetries, createdAt, sortBy, sortDir);
        int safePage = Math.max(page, 0);
        int safeLimit = Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);

        String requestSort = (sortBy == null || sortBy.isBlank()) ? DEFAULT_SORT_FIELD : sortBy;
        String safeSort = ALLOWED_SORT_FIELDS.contains(requestSort) ? requestSort : DEFAULT_SORT_FIELD;

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, safeSort);
        Pageable pageable = PageRequest.of(safePage, safeLimit, sort);
        Instant createdAtInstant = createdAt != null ? Instant.parse(createdAt) : null;
        Page<JobEventModel> result = dlqJobsRepository.searchDqlJobs(
                jobType,
                status,
                retryCount,
                maxRetries,
                createdAtInstant,
                pageable);

        log.info("Fetched {} DLQ jobs", result.getContent().size());

        return result;
    }

}
