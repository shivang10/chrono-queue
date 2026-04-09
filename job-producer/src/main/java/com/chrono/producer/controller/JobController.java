package com.chrono.producer.controller;

import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.service.JobEventProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/job")
@Tag(name = "Job Producer", description = "Job submission and management endpoints")
public class JobController {
    private final JobEventProducerService jobEventProducerService;

    public JobController(JobEventProducerService jobEventProducerService) {
        this.jobEventProducerService = jobEventProducerService;
        log.info("JobController initialized");
    }

    @PostMapping("/")
    @Operation(summary = "Create a new job", description = "Submit a new job to the queue for processing")
    public ResponseEntity<JobEventResponseDTO> createNewJob(@Valid @RequestBody JobEventRequestDTO jobEventRequestDTO) {
        log.info("API endpoint hit: POST /api/job/");
        JobEventResponseDTO response = jobEventProducerService.produceJobEvent(jobEventRequestDTO);
        log.info("API endpoint result: POST /api/job/ -> {}",
                ResponseEntity.accepted().build().getStatusCode().value());
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is running")
    public ResponseEntity<String> healthCheck() {
        log.info("API endpoint hit: GET /api/job/health");
        return ResponseEntity.ok("Service is up and running");
    }

}
