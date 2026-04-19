package com.chrono.worker.executor;

import com.chrono.common.enums.JobType;
import com.chrono.common.exceptions.JobExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JobExecutorRegistry {

    private final Map<JobType, JobExecutor> executors = new HashMap<>();

    public JobExecutorRegistry(List<JobExecutor> executors) {
        for (JobExecutor executor : executors) {
            this.executors.put(executor.supports(), executor);
            log.info("Registered executor {} for jobType: {}",
                    executor.getClass().getSimpleName(), executor.supports());
        }
    }

    public JobExecutor getExecutor(JobType jobType) {
        JobExecutor executor = executors.get(jobType);
        if (executor == null) {
            log.error("No executor registered for jobType: {}", jobType);
            throw new JobExecutionException(false, "No executor found for job type: " + jobType);
        }
        return executor;
    }
}