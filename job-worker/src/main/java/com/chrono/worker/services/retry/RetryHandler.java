package com.chrono.worker.services.retry;

import com.chrono.common.model.JobEventModel;
import com.chrono.worker.repository.redis.RetryRedisRepository;
import com.chrono.worker.services.dlq.DlqProducer;
import org.springframework.stereotype.Component;

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
            dlqProducer.send(job, ex);
            return;
        }

        int nextRetry = job.getRetryCount() + 1;
        if (nextRetry > job.getMaxRetries()) {
            dlqProducer.send(job, ex);
            return;
        }

        long delay = retryPolicy.nextDelay(nextRetry);
        long executeAt = System.currentTimeMillis() + delay;
        job.setExecuteAt(executeAt);
        job.setRetryCount(nextRetry);
        retryRedisRepository.scheduleRetry(job);
    }

}
