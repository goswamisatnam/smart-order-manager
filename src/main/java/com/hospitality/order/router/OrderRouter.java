package com.hospitality.order.router;

import com.hospitality.order.handler.OrderHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * WebFlux functional router configuration for order API endpoints.
 * Defines the contract-first API routes matching the OpenAPI specification.
 */
@Configuration
public class OrderRouter {

    private static final String API_BASE = "/api/v1";
    private static final String ORDERS_PATH = API_BASE + "/orders";
    private static final String ORDER_BY_ID_PATH = ORDERS_PATH + "/{orderId}";
    private static final String CANCEL_ORDER_PATH = ORDER_BY_ID_PATH + "/cancel";
    private static final String HEALTH_PATH = API_BASE + "/health";

    @Bean
    public RouterFunction<ServerResponse> orderRoutes(OrderHandler handler) {
        return RouterFunctions.route()
                .POST(ORDERS_PATH, accept(MediaType.APPLICATION_JSON), handler::createOrder)
                .GET(ORDERS_PATH, accept(MediaType.APPLICATION_JSON), handler::getAllOrders)
                .GET(ORDER_BY_ID_PATH, accept(MediaType.APPLICATION_JSON), handler::getOrderById)
                .PUT(ORDER_BY_ID_PATH, accept(MediaType.APPLICATION_JSON), handler::modifyOrder)
                .POST(CANCEL_ORDER_PATH, accept(MediaType.APPLICATION_JSON), handler::cancelOrder)
                .GET(HEALTH_PATH, handler::healthCheck)
                .build();
    }
}
