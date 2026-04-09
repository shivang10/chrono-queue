package com.chrono.dlq.repository;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.DlqJobDocumentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DlqJobsRepository extends MongoRepository<DlqJobDocumentModel, String> {

    @Query("""
            {
              $and: [
                ?#{ [0] == null ? {} : { 'jobType': [0] } },
                ?#{ [1] == null ? {} : { 'status': [1] } },
                ?#{ [2] == null ? {} : { 'retryCount': { $gte: [2] } } },
                ?#{ [3] == null ? {} : { 'maxRetries': [3] } },
                ?#{ [4] == null ? {} : { 'createdAt': { $gte: [4] } } }
              ]
            }
            """)
    Page<DlqJobDocumentModel> searchDqlJobs(
            JobType jobType,
            JobStatus status,
            Integer retryCount,
            Integer maxRetries,
            Instant createdAt,
            Pageable pageable);

    Optional<DlqJobDocumentModel> findByJobId(String jobId);

}