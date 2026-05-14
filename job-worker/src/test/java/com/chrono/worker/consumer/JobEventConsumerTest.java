package com.chrono.worker.consumer;

import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.common.model.payload.EmailJobPayloadModel;
import com.chrono.worker.config.WorkerValidationProperties;
import com.chrono.worker.services.JobProcessingService;
import com.chrono.worker.services.retry.RetryHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Mock
    private JobProcessingService jobProcessingService;

    @Mock
    private RetryHandler retryHandler;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private ObjectMapper consumerObjectMapper;

    @Test
    void malformedMessageDoesNotAbortBatchProcessing() throws Exception {
        WorkerValidationProperties properties = new WorkerValidationProperties();
        properties.setSimulatedFailureRate(0.0);

        JobEventConsumer consumer = new JobEventConsumer(
                consumerObjectMapper,
                jobProcessingService,
                retryHandler,
                properties,
                meterRegistry);

        JobEventModel validJob = JobEventModel.create(
                JobType.EMAIL,
                new EmailJobPayloadModel("user@example.com", "Welcome", "Hello"));
        String validJson = objectMapper.writeValueAsString(validJob);

        when(consumerObjectMapper.readValue(eq("{bad-json}"), eq(JobEventModel.class)))
                .thenThrow(new JsonProcessingException("bad json") {
                });
        when(consumerObjectMapper.readValue(eq(validJson), eq(JobEventModel.class))).thenReturn(validJob);
        doNothing().when(jobProcessingService).processJobEvent(validJob);

        consumer.consumeEmail(
                List.of("{bad-json}", validJson),
                List.of("email-jobs", "email-jobs"),
                List.of(0, 0),
                List.of(1L, 2L),
                List.of("key-1", "key-2"),
                acknowledgment);

        verify(jobProcessingService).processJobEvent(validJob);
        verify(acknowledgment).acknowledge();
    }
}