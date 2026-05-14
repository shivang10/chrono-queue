package com.chrono.retry.repository;

import com.chrono.common.exceptions.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryQueueRepositoryTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<List<String>> claimDueJobsScript;

    @Test
    void fetchDueJobsWrapsRedisFailureInInfrastructureException() {
        when(stringRedisTemplate.execute(eq(claimDueJobsScript), anyList(), any(), any()))
                .thenThrow(new RuntimeException("redis down"));

        RetryQueueRepository repository = new RetryQueueRepository(stringRedisTemplate, claimDueJobsScript);

        assertThrows(InfrastructureException.class, () -> repository.fetchDueJobs(25, 123456789L));
    }
}