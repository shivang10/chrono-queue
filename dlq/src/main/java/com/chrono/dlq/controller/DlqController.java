package com.chrono.dlq.controller;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.enums.JobType;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.service.DlqHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dlq")
public class DlqController {

    private final DlqHandlerService dlqHandlerService;

    public DlqController(DlqHandlerService dlqHandlerService) {
        this.dlqHandlerService = dlqHandlerService;
    }

    @GetMapping("/failed-jobs")
    public ResponseEntity<Page<JobEventModel>> getDlqJobs(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int limit,
                                                          @RequestParam(required = false) JobType jobType,
                                                          @RequestParam(required = false) JobStatus status,
                                                          @RequestParam(defaultValue = "failedAt") String sortBy,
                                                          @RequestParam(defaultValue = "desc") String sortDir) {
        log.info(
                "Received request to get DLQ jobs with page: {}, limit: {}, jobType: {}, status: {}, sortBy: {}, sortDir: {}",
                page, limit, jobType, status, sortBy, sortDir);
        return ResponseEntity.ok(dlqHandlerService.getDlqJobs(page, limit, jobType, status, sortBy, sortDir));
    }

}
