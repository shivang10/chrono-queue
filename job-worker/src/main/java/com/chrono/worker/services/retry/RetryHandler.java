package com.chrono.worker.services.retry;

import com.chrono.common.model.JobEventModel;
import com.chrono.worker.repository.redis.RetryRedisRepository;
import com.chrono.worker.services.dlq.DlqProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class RetryHandler {
    private final RetryRedisRepository retryRedisRepository;
    private final RetryPolicy retryPolicy;
    private final DlqProducer dlqProducer;

    public RetryHandler(
            RetryRedisRepository retryRedisRepository,
            RetryPolicy retryPolicy,
            DlqProducer dlqProducer) {
        this.retryRedisRepository = retryRedisRepository;
        this.retryPolicy = retryPolicy;
        this.dlqProducer = dlqProducer;
    }

    public void handleFailure(JobEventModel job, Exception ex) throws Exception {
        if (!retryPolicy.isRetryable(ex)) {
            log.warn("Job is non-retryable, routing to DLQ - jobId: {}, reason: {}",
                    job.getJobId(), ex.getMessage());
            dlqProducer.send(job, ex);
            return;
        }

        int nextRetry = job.getRetryCount() + 1;
        if (nextRetry > job.getMaxRetries()) {
            log.warn("Max retries exhausted, routing to DLQ - jobId: {}, retryCount: {}, maxRetries: {}",
                    job.getJobId(), job.getRetryCount(), job.getMaxRetries());
            dlqProducer.send(job, ex);
            return;
        }

        long delay = retryPolicy.nextDelay(nextRetry);
        Instant executeAt = Instant.now().plusMillis(delay);
        job.setExecuteAt(executeAt);
        job.setRetryCount(nextRetry);
        retryRedisRepository.scheduleRetry(job);
        log.info("Job scheduled for retry - jobId: {}, attempt: {}/{}, executeAt: {}, delayMs: {}",
                job.getJobId(), nextRetry, job.getMaxRetries(), executeAt, delay);
    }

}
