package com.chrono.worker.repository.redis;

import com.chrono.common.api.ErrorCode;
import com.chrono.common.constants.RedisKeys;
import com.chrono.common.exceptions.InfrastructureException;
import com.chrono.common.exceptions.JobPayloadSerializationException;
import com.chrono.common.model.JobEventModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class RetryRedisRepository {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> scheduleScript;
    private final DefaultRedisScript<Long> incrementScript;
    private final ObjectMapper objectMapper;

    public RetryRedisRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("scheduleScript") DefaultRedisScript<Long> scheduleScript,
            @Qualifier("incrementScript") DefaultRedisScript<Long> incrementScript, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.scheduleScript = scheduleScript;
        this.incrementScript = incrementScript;
        this.objectMapper = objectMapper;
    }

    public int incrementRetryCount(String jobId) {
        Long result = redisTemplate.execute(incrementScript, List.of(RedisKeys.RETRY_COUNT), jobId);
        int count = result != null ? result.intValue() : 0;
        log.debug("Retry count incremented - jobId: {}, newCount: {}", jobId, count);
        return count;
    }

    public void scheduleRetry(JobEventModel job) throws Exception {
        try {
            String payload = objectMapper.writeValueAsString(job);
            redisTemplate.execute(
                    scheduleScript,
                    List.of(RedisKeys.RETRY_ZSET, RedisKeys.RETRY_DATA),
                    job.getJobId(),
                    String.valueOf(job.getExecuteAt().toEpochMilli()),
                    payload);
            log.debug("Retry scheduled in Redis - jobId: {}, executeAt: {}",
                    job.getJobId(), job.getExecuteAt());
        } catch (JsonProcessingException ex) {
            throw new JobPayloadSerializationException(
                    "Failed to serialize job " + job.getJobId() + " for retry scheduling",
                    ex);
        } catch (RuntimeException ex) {
            throw new InfrastructureException(
                    ErrorCode.RETRY_SCHEDULING_FAILED,
                    "Failed to schedule retry for job " + job.getJobId(),
                    ex);
        }
    }
}
