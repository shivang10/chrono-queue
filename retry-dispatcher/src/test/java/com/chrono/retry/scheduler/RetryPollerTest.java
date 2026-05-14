package com.chrono.retry.scheduler;

import com.chrono.common.enums.JobType;
import com.chrono.common.api.ErrorCode;
import com.chrono.common.exceptions.InfrastructureException;
import com.chrono.common.model.JobEventModel;
import com.chrono.common.model.payload.EmailJobPayloadModel;
import com.chrono.retry.config.RetryPollerProperties;
import com.chrono.retry.producer.JobRequeueProducer;
import com.chrono.retry.repository.RetryQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryPollerTest {

    @Mock
    private RetryQueueRepository retryQueueRepository;

    @Mock
    private JobRequeueProducer jobRequeueProducer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private RetryPollerProperties retryPollerProperties;
    private RetryPoller retryPoller;

    @BeforeEach
    void setUp() {
        retryPollerProperties = new RetryPollerProperties();
        retryPollerProperties.setBatchSize(42);
        retryPollerProperties.setFixedDelay(1000);

        retryPoller = new RetryPoller(
                retryQueueRepository,
                jobRequeueProducer,
                objectMapper,
                retryPollerProperties,
                meterRegistry);
    }

    @Test
    void pollUsesConfiguredBatchSize() throws Exception {
        JobEventModel job = JobEventModel.create(
                JobType.EMAIL,
                new EmailJobPayloadModel("user@example.com", "Welcome", "Hello"));
        String jobJson = objectMapper.writeValueAsString(job);

        when(retryQueueRepository.fetchDueJobs(eq(42), anyLong())).thenReturn(List.of(jobJson));

        retryPoller.pollAndDispatch();

        verify(retryQueueRepository).fetchDueJobs(eq(42), anyLong());
        verify(jobRequeueProducer).requeue(job, jobJson);
    }

    @Test
    void pollReturnsCleanlyWhenRedisFetchFails() {
        when(retryQueueRepository.fetchDueJobs(eq(42), anyLong()))
                .thenThrow(new InfrastructureException(ErrorCode.RETRY_SCHEDULING_FAILED, "redis fetch failed"));

        assertDoesNotThrow(() -> retryPoller.pollAndDispatch());

        verify(jobRequeueProducer, never()).requeue(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
    }
}