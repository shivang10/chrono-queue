package com.chrono.worker.executor.impl;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebHookJobExecutor implements JobExecutor {

    @Override
    public JobType supports() {
        return JobType.WEBHOOK;
    }

    @Override
    public void execute(JobEventModel jobEvent) {
        log.info("Executing WEBHOOK job - jobId: {}", jobEvent.getJobId());
        log.debug("WEBHOOK job payload - jobId: {}, payload: {}",
                jobEvent.getJobId(), jobEvent.getPayload());
    }

}
