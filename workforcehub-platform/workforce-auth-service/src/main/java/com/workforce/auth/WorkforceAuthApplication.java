package com.workforce.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkforceAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceAuthApplication.class, args);
    }
}
