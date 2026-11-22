package com.cloudpulse;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class CloudPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPulseApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Enforce consistent UTC timestamps across entire application lifecycle
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
