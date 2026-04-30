package com.chrono.retry.producer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class JobRequeueProducer {

    private static final long SHUTDOWN_FLUSH_TIMEOUT_SECONDS = 30;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter requeueSuccessCounter;
    private final Counter requeueFailureCounter;
    private final AtomicInteger inFlightSends = new AtomicInteger(0);

    public JobRequeueProducer(KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.requeueSuccessCounter = Counter.builder("retry.producer.requeue.success.total")
                .description("Total jobs successfully requeued to Kafka")
                .register(meterRegistry);
        this.requeueFailureCounter = Counter.builder("retry.producer.requeue.failure.total")
                .description("Total jobs that failed to requeue to Kafka")
                .register(meterRegistry);
        meterRegistry.gauge("retry.producer.inflight.sends", inFlightSends);
    }

    public void requeue(JobEventModel job, String jobEventJson) {
        String topicName = KafkaTopics.getTopicForJobType(job.getJobType());

        if (topicName == null || jobEventJson == null) {
            log.error("Failed to requeue job: topicName or serialized payload is null - jobId: {}", job.getJobId());
            requeueFailureCounter.increment();
            return;
        }

        inFlightSends.incrementAndGet();
        kafkaTemplate.send(topicName, job.getJobId(), jobEventJson)
                .whenComplete((result, exception) -> {
                    inFlightSends.decrementAndGet();
                    if (exception != null) {
                        requeueFailureCounter.increment();
                        log.error("Failed to requeue job - jobId: {}, topic: {}", job.getJobId(), topicName, exception);
                    } else {
                        requeueSuccessCounter.increment();
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

    @PreDestroy
    public void flushOnShutdown() {
        log.info("Shutting down JobRequeueProducer - flushing pending Kafka sends, inFlight: {}",
                inFlightSends.get());
        kafkaTemplate.flush();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SHUTDOWN_FLUSH_TIMEOUT_SECONDS);
            while (inFlightSends.get() > 0 && System.nanoTime() < deadline) {
                Thread.sleep(100);
            }
            if (inFlightSends.get() > 0) {
                log.warn("Shutdown timeout reached with {} in-flight sends still pending", inFlightSends.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for in-flight sends to complete");
        }
        log.info("JobRequeueProducer shutdown complete");
    }
}
