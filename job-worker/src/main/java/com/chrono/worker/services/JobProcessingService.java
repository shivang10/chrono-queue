package com.chrono.worker.services;

import com.chrono.common.model.JobEventModel;
import com.chrono.worker.executor.JobExecutor;
import com.chrono.worker.executor.JobExecutorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobProcessingService {
    private final JobExecutorRegistry jobExecutorRegistry;

    public JobProcessingService(JobExecutorRegistry jobExecutorRegistry) {
        this.jobExecutorRegistry = jobExecutorRegistry;
    }

    public void processJobEvent(JobEventModel jobEvent) {
        log.debug("Dispatching jobId: {} to executor for jobType: {}",
                jobEvent.getJobId(), jobEvent.getJobType());
        JobExecutor executor = jobExecutorRegistry.getExecutor(jobEvent.getJobType());
        executor.execute(jobEvent);
    }
}
