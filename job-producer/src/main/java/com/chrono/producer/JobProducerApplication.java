package com.chrono.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.chrono")
public class JobProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobProducerApplication.class, args);
    }

}
