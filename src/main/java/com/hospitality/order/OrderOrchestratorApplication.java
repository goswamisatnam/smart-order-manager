package com.hospitality.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smart Order Manager - Hospitality Order Orchestrator Application.
 * <p>
 * Uses Spring Boot WebFlux for reactive APIs and Apache Camel Saga
 * for distributed transaction orchestration following the Microservice
 * Orchestrator pattern and Traveler MMF specification.
 */
@SpringBootApplication
public class OrderOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderOrchestratorApplication.class, args);
    }
}
