package com.chrono.common.model;

import java.util.Map;

import com.chrono.common.enums.JobStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEvent {
    
    private String jobId;
    private String jobType;
    
    private long createdAt;
    
    private Map<String, Object> payload;
    
    private int retryCount;
    private int maxRetries;

    private JobStatus status;
}
