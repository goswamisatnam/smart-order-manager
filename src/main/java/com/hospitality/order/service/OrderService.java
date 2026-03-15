package com.hospitality.order.service;

import com.hospitality.order.dto.CancelOrderRequest;
import com.hospitality.order.dto.CreateOrderRequest;
import com.hospitality.order.dto.ModifyOrderRequest;
import com.hospitality.order.dto.OrderResponse;
import com.hospitality.order.model.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for order management operations.
 */
public interface OrderService {

    Mono<OrderResponse> createOrder(CreateOrderRequest request);

    Mono<OrderResponse> modifyOrder(String orderId, ModifyOrderRequest request);

    Mono<OrderResponse> cancelOrder(String orderId, CancelOrderRequest request);

    Mono<OrderResponse> getOrderById(String orderId);

    Flux<OrderResponse> getAllOrders(String travelerId, OrderStatus status);
}
