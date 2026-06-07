package com.example.concurrencylab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ConcurrencyLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcurrencyLabApplication.class, args);
    }
}
