package com.chrono.producer.controller;

import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.service.JobEventProducerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;


@RestController
@RequestMapping("/api/job")
public class JobController {
    private static final Logger logger = Logger.getLogger(JobController.class.getName());
    private final JobEventProducerService jobEventProducerService;

    public JobController(JobEventProducerService jobEventProducerService) {
        this.jobEventProducerService = jobEventProducerService;
        logger.info("JobController initialized");
    }

    @PostMapping("/")
    public ResponseEntity<JobEventResponseDTO> createNewJob(@RequestBody JobEventRequestDTO jobEventRequestDTO) {
        JobEventResponseDTO response = jobEventProducerService.produceJobEvent(jobEventRequestDTO);
        if (response.getMessage().contains("successfully")) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/health")
    public String getMethodName(@RequestParam String param) {
        return ResponseEntity.ok("Service is up and running").getBody();
    }


}
