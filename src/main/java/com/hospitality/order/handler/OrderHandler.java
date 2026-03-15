package com.hospitality.order.handler;

import com.hospitality.order.dto.CancelOrderRequest;
import com.hospitality.order.dto.CreateOrderRequest;
import com.hospitality.order.dto.ModifyOrderRequest;
import com.hospitality.order.dto.OrderResponse;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebFlux functional handler for order API endpoints.
 */
@Component
public class OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderHandler.class);

    private final OrderService orderService;

    public OrderHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    public Mono<ServerResponse> createOrder(ServerRequest request) {
        return request.bodyToMono(CreateOrderRequest.class)
                .flatMap(orderService::createOrder)
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request.path()));
    }

    public Mono<ServerResponse> getOrderById(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        return orderService.getOrderById(orderId)
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> getAllOrders(ServerRequest request) {
        String travelerId = request.queryParam("travelerId").orElse(null);
        OrderStatus status = request.queryParam("status")
                .map(OrderStatus::valueOf)
                .orElse(null);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderService.getAllOrders(travelerId, status), OrderResponse.class);
    }

    public Mono<ServerResponse> modifyOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        return request.bodyToMono(ModifyOrderRequest.class)
                .flatMap(modifyRequest -> orderService.modifyOrder(orderId, modifyRequest))
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> cancelOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        return request.bodyToMono(CancelOrderRequest.class)
                .defaultIfEmpty(new CancelOrderRequest())
                .flatMap(cancelRequest -> orderService.cancelOrder(orderId, cancelRequest))
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "order-orchestrator",
                "timestamp", LocalDateTime.now().toString()
        );
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(health);
    }

    private Mono<ServerResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        Map<String, Object> errorBody = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path
        );
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorBody);
    }
}
