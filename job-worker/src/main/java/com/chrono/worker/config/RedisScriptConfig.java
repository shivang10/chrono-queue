package com.chrono.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisScriptConfig {

    @Bean(name = "scheduleScript")
    public DefaultRedisScript<Long> scheduleRetryScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("redis/scripts/schedule_retry.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean(name = "incrementScript")
    public DefaultRedisScript<Long> incrementRetryScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("redis/scripts/increment_retry.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
