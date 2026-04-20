package com.chrono.dlq.repository;

import com.chrono.common.model.DlqJobDocumentModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DlqJobsRepository extends MongoRepository<DlqJobDocumentModel, String>, DlqJobsRepositoryCustom {

    Optional<DlqJobDocumentModel> findByJobId(String jobId);

}