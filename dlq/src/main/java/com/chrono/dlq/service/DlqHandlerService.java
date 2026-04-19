package com.chrono.dlq.service;

import com.chrono.common.api.ErrorCode;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.exceptions.InfrastructureException;
import com.chrono.common.exceptions.InvalidJobRequestException;
import com.chrono.common.model.DlqJobDocumentModel;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.repository.DlqJobsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
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
        log.debug("Processing DLQ message payload - jobId: {}, jobType: {}, topic: {}",
                jobEvent.getJobId(), jobEvent.getJobType(), topic);
    }

    public void saveFailedJob(JobEventModel jobEvent) {
        if (jobEvent == null) {
            throw new InvalidJobRequestException("Failed job payload cannot be null");
        }

        try {
            DlqJobDocumentModel dlqJob = Objects.requireNonNull(
                    convertToDlqJobDocument(jobEvent),
                    "Converted DLQ document cannot be null");
            dlqJobsRepository.save(dlqJob);
            log.info("Failed job persisted to DLQ repository - jobId: {}, jobType: {}, retryCount: {}/{}",
                    jobEvent.getJobId(), jobEvent.getJobType(),
                    jobEvent.getRetryCount(), jobEvent.getMaxRetries());
        } catch (RuntimeException ex) {
            throw new InfrastructureException(
                    ErrorCode.DLQ_PERSISTENCE_FAILED,
                    "Failed to persist job " + jobEvent.getJobId() + " to the DLQ",
                    ex);
        }
    }

    private @NonNull DlqJobDocumentModel convertToDlqJobDocument(@NonNull JobEventModel jobEvent) {
        DlqJobDocumentModel dlqJob = new DlqJobDocumentModel();
        dlqJob.setJobId(jobEvent.getJobId());
        dlqJob.setJobType(jobEvent.getJobType());
        dlqJob.setEventVersion(jobEvent.getEventVersion());
        dlqJob.setPayload(jobEvent.getPayload());
        dlqJob.setRetryCount(jobEvent.getRetryCount());
        dlqJob.setMaxRetries(jobEvent.getMaxRetries());
        dlqJob.setStatus(JobStatus.FAILED);
        dlqJob.setExecuteAt(Instant.now());
        return dlqJob;
    }

    public Page<DlqJobDocumentModel> getDlqJobs(
            int page,
            int limit,
            JobType jobType,
            JobStatus status,
            Integer retryCount,
            Integer maxRetries,
            String createdAt,
            String sortBy,
            String sortDir) {
        log.debug(
                "Querying DLQ jobs - page: {}, limit: {}, jobType: {}, status: {}, retryCount: {}, maxRetries: {}, createdAt: {}, sortBy: {}, sortDir: {}",
                page, limit, jobType, status, retryCount, maxRetries, createdAt, sortBy, sortDir);
        validateRetryFilterRange(retryCount, maxRetries);
        int safePage = Math.max(page, 0);
        int safeLimit = Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);

        String safeSort = resolveSortField(sortBy);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, safeSort);
        Pageable pageable = PageRequest.of(safePage, safeLimit, sort);
        Instant createdAtInstant = parseCreatedAt(createdAt);
        Page<DlqJobDocumentModel> result = dlqJobsRepository.searchDqlJobs(
                jobType,
                status,
                retryCount,
                maxRetries,
                createdAtInstant,
                pageable);

        log.info("DLQ query returned {} of {} total jobs", result.getContent().size(), result.getTotalElements());

        return result;
    }

    private void validateRetryFilterRange(Integer retryCount, Integer maxRetries) {
        if (retryCount != null && retryCount < 0) {
            throw new InvalidJobRequestException("retryCount cannot be negative");
        }

        if (maxRetries != null && maxRetries < 0) {
            throw new InvalidJobRequestException("maxRetries cannot be negative");
        }
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_FIELD;
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidJobRequestException("Unsupported sortBy field: " + sortBy);
        }

        return sortBy;
    }

    private Instant parseCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(createdAt);
        } catch (DateTimeParseException ex) {
            throw new InvalidJobRequestException("createdAt must be an ISO-8601 instant", ex);
        }
    }

}
