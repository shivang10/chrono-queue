package com.chrono.retry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @SuppressWarnings("unchecked")
    @Bean(name = "claimDueJobsScript")
    public DefaultRedisScript<List<String>> claimDueJobsScript() {
        DefaultRedisScript script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("redis/scripts/claim_due_jobs.lua")));
        script.setResultType(List.class);
        return script;
    }
}
