package com.chrono.producer.dto.jobEventDto;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.payload.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEventRequestDTO {

    @NotNull(message = "Job Type cannot be blank")
    private JobType jobType;

    @NotNull(message = "Payload cannot be blank")
    @Valid
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "jobType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = WebhookJobPayloadModel.class, name = "WEBHOOK"),
            @JsonSubTypes.Type(value = EmailJobPayloadModel.class, name = "EMAIL"),
            @JsonSubTypes.Type(value = PaymentProcessingJobPayloadModel.class, name = "PAYMENT_PROCESSING"),
            @JsonSubTypes.Type(value = OrderCancellationJobPayloadModel.class, name = "ORDER_CANCELLATION")
    })
    private JobPayloadModel payload;
}
