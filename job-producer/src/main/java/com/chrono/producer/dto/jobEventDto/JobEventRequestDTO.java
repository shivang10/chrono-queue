package com.chrono.producer.dto.jobEventDto;


import com.chrono.common.enums.JobType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventRequestDTO {

    @NotNull(message = "Job Type cannot be blank")
    private JobType jobType;

    @NotEmpty(message = "Payload cannot be blank")
    private Map<String, Object> payload;
}
