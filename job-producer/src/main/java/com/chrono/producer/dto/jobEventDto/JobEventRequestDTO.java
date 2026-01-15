package com.chrono.producer.dto.jobEventDto;


import java.util.Map;

import com.chrono.common.enums.JobType;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventRequestDTO {
    
    @NotBlank(message = "Job Type cannot be blank")
    private JobType jobType;

    @NotBlank(message = "Payload cannot be blank")
    private Map<String, Object> payload;

}
