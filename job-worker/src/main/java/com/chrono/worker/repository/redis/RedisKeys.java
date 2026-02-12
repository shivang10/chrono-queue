package com.chrono.worker.repository.redis;

public final class RedisKeys {

    private RedisKeys() {}

    public static final String RETRY_ZSET = "retry:zset";
    public static final String RETRY_DATA = "retry:data";
    public static final String RETRY_COUNT = "retry:count";
}
