package com.chrono.common.constants;

public class JobConstants {

    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long DEFAULT_RETRY_DELAY_MS = 5000; // 5 seconds
    public static final int MAX_PAYLOAD_SIZE_BYTES = 1024 * 1024; // 1 MB

    private JobConstants() {
        throw new AssertionError("Cannot instantiate JobConstants - constant class.");
    }
}
