package com.chrono.worker.executor.impl;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class OrderCancellationJobExecutor implements JobExecutor {

    private static final Logger logger = Logger.getLogger(OrderCancellationJobExecutor.class.getName());

    @Override
    public JobType supports() {
        return JobType.ORDER_CANCELLATION;
    }

    @Override
    public void execute(JobEventModel jobEvent) {
        logger.info(String.format("Processing order cancellation - JobId: %s, Payload: %s",
                jobEvent.getJobId(), jobEvent.getPayload()));
    }
}
