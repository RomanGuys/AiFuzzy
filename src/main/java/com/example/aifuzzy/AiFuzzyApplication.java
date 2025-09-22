package com.example.aifuzzy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiFuzzyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFuzzyApplication.class, args);
    }
}
