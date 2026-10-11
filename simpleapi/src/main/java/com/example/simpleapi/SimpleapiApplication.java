package com.example.simpleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SimpleapiApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                SimpleapiApplication.class,
                args
        );
    }
}