package com.chrono.worker.executor.impl;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCancellationJobExecutor implements JobExecutor {

    @Override
    public JobType supports() {
        return JobType.ORDER_CANCELLATION;
    }

    @Override
    public void execute(JobEventModel jobEvent) {
        log.info("Executing ORDER_CANCELLATION job - jobId: {}", jobEvent.getJobId());
        log.debug("ORDER_CANCELLATION job payload - jobId: {}, payload: {}",
                jobEvent.getJobId(), jobEvent.getPayload());
    }
}
