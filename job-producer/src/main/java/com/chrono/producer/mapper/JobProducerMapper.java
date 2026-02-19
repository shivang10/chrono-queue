package com.chrono.producer.mapper;

import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import org.springframework.stereotype.Component;

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
        jobEventResponseDTO.setJobType(jobEventModel.getJobType());
        return jobEventResponseDTO;
    }
}
