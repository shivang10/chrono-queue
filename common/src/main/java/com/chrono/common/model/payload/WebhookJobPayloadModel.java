package com.chrono.common.model.payload;

import com.chrono.common.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebhookJobPayloadModel extends JobPayloadModel {

    @NotBlank(message = "Webhook url cannot be blank")
    private String url;

    private String method;
    private String body;

    @Override
    public JobType supportedJobType() {
        return JobType.WEBHOOK;
    }
}
