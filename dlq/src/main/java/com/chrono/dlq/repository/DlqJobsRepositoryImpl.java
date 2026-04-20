package com.chrono.dlq.repository;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.DlqJobDocumentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DlqJobsRepositoryImpl implements DlqJobsRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public DlqJobsRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<DlqJobDocumentModel> searchDqlJobs(
            JobType jobType,
            JobStatus status,
            Integer retryCount,
            Integer maxRetries,
            Instant createdAt,
            Pageable pageable) {

        List<Criteria> filters = new ArrayList<>();

        if (jobType != null) {
            filters.add(Criteria.where("job_type").is(jobType));
        }
        if (status != null) {
            filters.add(Criteria.where("status").is(status));
        }
        if (retryCount != null) {
            filters.add(Criteria.where("retry_count").gte(retryCount));
        }
        if (maxRetries != null) {
            filters.add(Criteria.where("max_retries").is(maxRetries));
        }
        if (createdAt != null) {
            filters.add(Criteria.where("created_at").gte(createdAt));
        }

        Query query = filters.isEmpty()
                ? new Query()
                : new Query(new Criteria().andOperator(filters));

        long total = mongoTemplate.count(query, DlqJobDocumentModel.class);

        query.with(Objects.requireNonNull(pageable));
        List<DlqJobDocumentModel> results = mongoTemplate.find(query, DlqJobDocumentModel.class);

        return new PageImpl<>(results, Objects.requireNonNull(pageable), total);
    }
}
