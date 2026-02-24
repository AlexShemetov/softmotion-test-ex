package com.example.testex.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.testex")
public class TestexApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestexApplication.class, args);
    }
}
