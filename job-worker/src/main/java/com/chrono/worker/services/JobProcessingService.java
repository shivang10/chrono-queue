package com.chrono.worker.services;

import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import com.chrono.worker.executor.JobExecutorRegistry;
import org.springframework.stereotype.Service;


@Service
public class JobProcessingService {
    private final JobExecutorRegistry jobExecutorRegistry;

    public JobProcessingService(JobExecutorRegistry jobExecutorRegistry) {
        this.jobExecutorRegistry = jobExecutorRegistry;
    }

    public void processJobEvent(JobEventModel jobEvent) {
        JobExecutor executor = jobExecutorRegistry.getExecutor(jobEvent.getJobType());
        executor.execute(jobEvent);
    }
}
