package com.teamfourpixels.deepreaderbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeepReaderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepReaderBackendApplication.class, args);
    }
}
