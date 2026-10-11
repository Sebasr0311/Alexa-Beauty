package com.alexabeauty;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AlexaBeautyApplication {

    @PostConstruct
    void init() {
        // Enforce UTC internally across JVM for database consistency
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AlexaBeautyApplication.class, args);
    }
}
