package com.chrono.worker.executor.impl;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentProcessingJobExecutor implements JobExecutor {

    @Override
    public JobType supports() {
        return JobType.PAYMENT_PROCESSING;
    }

    @Override
    public void execute(JobEventModel jobEvent) {
        log.info("Executing PAYMENT_PROCESSING job - jobId: {}", jobEvent.getJobId());
        log.debug("PAYMENT_PROCESSING job payload - jobId: {}, payload: {}",
                jobEvent.getJobId(), jobEvent.getPayload());
    }
}
