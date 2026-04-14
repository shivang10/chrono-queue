package com.chrono.common.model.payload;

import com.chrono.common.enums.JobType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentProcessingJobPayloadModel extends JobPayloadModel {

    @NotBlank(message = "paymentId cannot be blank")
    private String paymentId;

    @NotBlank(message = "currency cannot be blank")
    private String currency;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @Override
    public JobType supportedJobType() {
        return JobType.PAYMENT_PROCESSING;
    }
}
