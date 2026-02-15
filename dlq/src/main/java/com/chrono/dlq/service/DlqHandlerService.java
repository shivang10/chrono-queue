package com.chrono.dlq.service;

import org.springframework.stereotype.Service;

import java.util.logging.Logger;

import com.chrono.common.model.JobEventModel;



@Service
public class DlqHandlerService {
    private static final Logger logger = Logger.getLogger(DlqHandlerService.class.getName());

    public void handleDlqMessage(JobEventModel jobEvent, String topic) {
        logger.info(String.format("Handling DLQ message for job: %s from topic: %s", 
                jobEvent.getJobId(), topic));
    }

}
