package com.chrono.retry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RetryDispatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryDispatcherApplication.class, args);
    }

}
