package com.chrono.worker.executor;

import com.chrono.common.enums.JobType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class JobExecutorRegistry {

    private final Map<JobType, JobExecutor> executors = new HashMap<>();


    public JobExecutorRegistry(List<JobExecutor> executors) {
        for (JobExecutor executor : executors) {
            this.executors.put(executor.supports(), executor);
        }
    }

    public JobExecutor getExecutor(JobType jobType) {
        JobExecutor executor = executors.get(jobType);
        if (executor == null) {
            throw new IllegalArgumentException("No executor found for job type: " + jobType);
        }
        return executor;
    }
}