package com.chrono.producer.dto.jobEventDto;

import com.chrono.common.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventResponseDTO {
    private String message;
    private String jobId;
    private JobType jobType;
    private int failUntilAttempt;

    public JobEventResponseDTO(String message) {
        this.message = message;
    }
}
