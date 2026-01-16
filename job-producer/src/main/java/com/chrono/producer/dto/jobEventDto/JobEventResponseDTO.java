package com.chrono.producer.dto.jobEventDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventResponseDTO {
    private String message;
    private String jobId;
    
    public JobEventResponseDTO(String message) {
        this.message = message;
    }
}
