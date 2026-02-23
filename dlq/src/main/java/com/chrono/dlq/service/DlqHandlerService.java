package com.chrono.dlq.service;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.repository.DlqJobsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DlqHandlerService {

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

    public Page<JobEventModel> getDlqJobs(int page,
                                          int limit,
                                          JobType jobType,
                                          JobStatus status,
                                          String sortBy,
                                          String sortDir) {
        log.info("Fetching DLQ jobs with page: {} and limit: {}", page, limit);
        if (limit > 100) {
            limit = 100;
        }

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Query query = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList<>();

        if (jobType != null) {
            criteriaList.add(Criteria.where("jobType").is(jobType));
        }
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<JobEventModel> jobs = dlqJobsRepository.findAll(pageable).getContent();
        log.info("Fetched {} DLQ jobs", jobs.size());
        long total = dlqJobsRepository.count();

        return new PageImpl<>(jobs, pageable, total);
    }

}
