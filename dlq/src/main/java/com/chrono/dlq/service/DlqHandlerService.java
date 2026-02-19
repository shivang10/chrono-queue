package com.chrono.dlq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.chrono.common.model.JobEventModel;

@Slf4j
@Service
public class DlqHandlerService {

    public void handleDlqMessage(JobEventModel jobEvent, String topic) {
        log.info("Handling DLQ message for job: {} from topic: {}",
                jobEvent.getJobId(), topic);
    }

}
