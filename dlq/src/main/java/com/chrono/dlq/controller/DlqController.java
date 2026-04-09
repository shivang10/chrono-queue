package com.chrono.dlq.controller;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.service.DlqHandlerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/dlq")
public class DlqController {

    private final DlqHandlerService dlqHandlerService;

    public DlqController(DlqHandlerService dlqHandlerService) {
        this.dlqHandlerService = dlqHandlerService;
    }

    @GetMapping("/failed-jobs")
    public ResponseEntity<Page<JobEventModel>> getDlqJobs(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
                                                          @RequestParam(required = false) JobType jobType,
                                                          @RequestParam(required = false) JobStatus status,
                                                          @RequestParam(required = false) Integer retryCount,
                                                          @RequestParam(required = false) Integer maxRetries,
                                                          @RequestParam(required = false) String createdAt,
                                                          @RequestParam(required = false) String sortBy,
                                                          @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc", flags = Pattern.Flag.CASE_INSENSITIVE, message = "sortDir must be either asc or desc") String sortDir) {
        log.info(
                "Received request to get DLQ jobs with page: {}, limit: {}, jobType: {}, status: {}, retryCount: {}, maxRetries: {}, createdAt: {}, sortBy: {}, sortDir: {}",
                page, limit, jobType, status, retryCount, maxRetries, createdAt, sortBy, sortDir);
        return ResponseEntity.ok(
                dlqHandlerService.getDlqJobs(page, limit, jobType, status, retryCount, maxRetries, createdAt, sortBy,
                        sortDir));
    }

}
