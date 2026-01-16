package com.chrono.producer.mapper;


import org.springframework.stereotype.Component;

import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;

@Component
public class JobProducerMapper {
    public JobProducerMapper() {
    }

    public JobEventModel toJobEvent(JobEventRequestDTO dto) {
        return JobEventModel.create(dto.getJobType(), dto.getPayload());
    }


    public JobEventResponseDTO toJobEventResponse(JobEventModel jobEventModel) {
        JobEventResponseDTO jobEventResponseDTO = new JobEventResponseDTO();
        jobEventResponseDTO.setMessage("Job producer response created successfully");
        jobEventResponseDTO.setJobId(jobEventModel.getJobId());
        return jobEventResponseDTO;
    }
}
