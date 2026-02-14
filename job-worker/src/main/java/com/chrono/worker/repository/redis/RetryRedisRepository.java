package com.chrono.worker.repository.redis;

import com.chrono.common.constants.RedisKeys;
import com.chrono.common.model.JobEventModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

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
        return result != null ? result.intValue() : 0;
    }

    public void scheduleRetry(JobEventModel job) throws Exception {
        String payload = objectMapper.writeValueAsString(job);
        redisTemplate.execute(
                scheduleScript,
                List.of(RedisKeys.RETRY_ZSET, RedisKeys.RETRY_DATA),
                job.getJobId(),
                String.valueOf(job.getExecuteAt()),
                payload
        );
    }
}
