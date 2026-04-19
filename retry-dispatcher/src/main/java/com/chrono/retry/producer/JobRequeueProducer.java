package com.chrono.retry.producer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobRequeueProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public JobRequeueProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void requeue(JobEventModel job, String jobEventJson) {
        String topicName = KafkaTopics.getTopicForJobType(job.getJobType());

        if (topicName == null || jobEventJson == null) {
            log.warn("Cannot requeue job: topicName or serialized payload is null - jobId: {}", job.getJobId());
            return;
        }

        kafkaTemplate.send(topicName, job.getJobId(), jobEventJson)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to requeue job - jobId: {}, topic: {}", job.getJobId(), topicName, exception);
                    } else {
                        log.info(
                                "Job requeued successfully - jobId: {}, topic: {}, partition: {}, offset: {}, retryCount: {}",
                                job.getJobId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                job.getRetryCount());
                    }
                });
    }
}
