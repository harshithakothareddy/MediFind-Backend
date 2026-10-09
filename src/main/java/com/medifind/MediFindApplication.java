package com.medifind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediFindApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediFindApplication.class, args);
    }
}
