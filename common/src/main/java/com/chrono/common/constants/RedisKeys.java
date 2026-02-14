package com.chrono.common.constants;

public final class RedisKeys {

    public static final String RETRY_ZSET = "retry:zset";
    public static final String RETRY_DATA = "retry:data";
    public static final String RETRY_COUNT = "retry:count";

    private RedisKeys() {
    }
}
