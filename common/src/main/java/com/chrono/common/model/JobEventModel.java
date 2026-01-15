package com.chrono.common.model;

import java.time.LocalDateTime;
import java.util.Map;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEventModel {
    
    private String jobId;
    private JobType jobType;
    
    private LocalDateTime createdAt;
    
    private Map<String, Object> payload;
    
    private int retryCount;
    private int maxRetries;

    private JobStatus status;
}
