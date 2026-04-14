package com.chrono.common.model.payload;

import com.chrono.common.enums.JobType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailJobPayloadModel extends JobPayloadModel {

    @NotBlank(message = "Email recipient cannot be blank")
    @Email(message = "Email recipient must be a valid email address")
    private String recipient;

    private String subject;
    private String body;

    @Override
    public JobType supportedJobType() {
        return JobType.EMAIL;
    }
}
