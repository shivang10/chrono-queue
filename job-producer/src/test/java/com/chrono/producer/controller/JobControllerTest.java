package com.chrono.producer.controller;

import com.chrono.common.api.GlobalExceptionHandler;
import com.chrono.common.enums.JobType;
import com.chrono.common.exceptions.JobPublishException;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.dto.jobEventDto.JobSubmissionStatus;
import com.chrono.producer.service.JobEventProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(GlobalExceptionHandler.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobEventProducerService jobEventProducerService;

    @Test
    void createNewJobReturnsAcceptedResponse() throws Exception {
        JobEventResponseDTO response = new JobEventResponseDTO();
        response.setStatus(JobSubmissionStatus.ACCEPTED);
        response.setMessage("Job accepted for processing");
        response.setJobId("job-123");
        response.setJobType(JobType.EMAIL);

        when(jobEventProducerService.produceJobEvent(any())).thenReturn(response);

        mockMvc.perform(post("/api/job/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobType", "EMAIL",
                                "payload", Map.of("recipient", "user@example.com")))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message").value("Job accepted for processing"))
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.jobType").value("EMAIL"));
    }

    @Test
    void createNewJobReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/api/job/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payload", Map.of("recipient", "user@example.com")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/job/"))
                .andExpect(jsonPath("$.validationErrors.jobType").value("Job Type cannot be blank"));
    }

    @Test
    void createNewJobReturnsStructuredInfrastructureError() throws Exception {
        when(jobEventProducerService.produceJobEvent(any()))
                .thenThrow(new JobPublishException("Failed to publish job event to Kafka"));

        mockMvc.perform(post("/api/job/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobType", "EMAIL",
                                "payload", Map.of("recipient", "user@example.com")))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("JOB_PUBLISH_FAILED"))
                .andExpect(jsonPath("$.message").value("Failed to publish job event to Kafka"))
                .andExpect(jsonPath("$.path").value("/api/job/"));
    }
}