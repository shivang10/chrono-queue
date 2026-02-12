package com.chrono.worker.executor.impl;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class PaymentProcessingJobExecutor implements JobExecutor {

    private static final Logger logger = Logger.getLogger(PaymentProcessingJobExecutor.class.getName());

    @Override
    public JobType supports() {
        return JobType.PAYMENT_PROCESSING;
    }

    @Override
    public void execute(JobEventModel jobEvent) {
        logger.info(String.format("Processing payment - JobId: %s, Payload: %s",
                jobEvent.getJobId(), jobEvent.getPayload()));
    }
}
