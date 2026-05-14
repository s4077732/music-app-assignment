package com.amazonaws.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Main entry point for the Spring Boot backend application.
 * This class starts the REST API used by the frontend when the backend
 * is deployed on EC2 or ECS.
 */
@SpringBootApplication
public class MusicAppApplication {

    public static void main(String[] args) {
        // Start the Spring Boot application and run the embedded web server.
        SpringApplication.run(MusicAppApplication.class, args);
    }
}