package com.chrono.worker.executor.impl;

import org.springframework.stereotype.Component;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;


@Component
public class WebHookJobExecutor implements  JobExecutor {

    @Override
    public JobType supports() {
        return JobType.WEBHOOK;
    }

    @Override
    public void execute(JobEventModel jobEvent) {
        System.out.println("Executing WebHook - JobId: " + jobEvent.getJobId() + ", Payload: " + jobEvent.getPayload() + ", ID: " + jobEvent.getJobId());
    }
    
}
