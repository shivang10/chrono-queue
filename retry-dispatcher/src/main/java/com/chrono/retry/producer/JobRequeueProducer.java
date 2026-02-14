package com.chrono.retry.producer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JobRequeueProducer {

    private static final Logger logger = Logger.getLogger(JobRequeueProducer.class.getName());
    private final KafkaTemplate<String, String> kafkaTemplate;

    public JobRequeueProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void requeue(JobEventModel job, String jobEventJson) {
        String topicName = KafkaTopics.getTopicForJobType(job.getJobType());

        if (topicName == null || jobEventJson == null) {
            logger.severe("Cannot send to Kafka: topicName or value is null for job: " + job.getJobId());
            return;
        }

        kafkaTemplate.send(topicName, job.getJobId(), jobEventJson)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.log(Level.SEVERE, String.format(
                                "Failed to requeue job %s to topic %s", job.getJobId(), topicName), exception);
                    } else {
                        logger.info(String.format(
                                "Requeued job %s - Topic: %s, Partition: %d, Offset: %d, RetryCount: %d",
                                job.getJobId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                job.getRetryCount()));
                    }
                });
    }
}
