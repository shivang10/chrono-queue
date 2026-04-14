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
public class OrderCancellationJobPayloadModel extends JobPayloadModel {

    @NotBlank(message = "orderId cannot be blank")
    private String orderId;

    private String reason;

    @Override
    public JobType supportedJobType() {
        return JobType.ORDER_CANCELLATION;
    }
}
