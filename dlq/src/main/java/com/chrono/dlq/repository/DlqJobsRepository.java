package com.chrono.dlq.repository;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DlqJobsRepository extends MongoRepository<JobEventModel, String> {
    Page<JobEventModel> findAll(Pageable pageable);

    Page<JobEventModel> findByJobTypeAndStatus(JobType jobType, JobStatus status, Pageable pageable);

    Optional<JobEventModel> findByJobId(String jobId);

}