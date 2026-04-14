package com.chrono.common.model.payload;

import com.chrono.common.enums.JobType;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class JobPayloadModel {

    @Min(value = 1, message = "Payload schemaVersion must be >= 1")
    private int schemaVersion = 1;

    public abstract JobType supportedJobType();
}
