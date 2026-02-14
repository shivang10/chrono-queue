package com.chrono.retry.repository;

import com.chrono.common.constants.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RetryQueueRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List<String>> claimDueJobsScript;

    public RetryQueueRepository(StringRedisTemplate stringRedisTemplate,
                                @Qualifier("claimDueJobsScript") DefaultRedisScript<List<String>> claimDueJobsScript,
                                ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.claimDueJobsScript = claimDueJobsScript;
    }

    public List<String> fetchDueJobs(int batchSize, long currentTimeMillis) {
        List<String> jobsStrings = stringRedisTemplate.execute(claimDueJobsScript, List.of(RedisKeys.RETRY_ZSET, RedisKeys.RETRY_DATA), String.valueOf(currentTimeMillis), String.valueOf(batchSize));
        return jobsStrings != null ? jobsStrings : List.of();
    }
}
