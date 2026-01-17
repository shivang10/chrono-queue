package com.chrono.worker.executor;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;

public interface JobExecutor {
    JobType supports();
    void execute(JobEventModel jobEvent);
}
