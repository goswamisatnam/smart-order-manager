package com.hospitality.order.repository;

import com.hospitality.order.model.Order;
import com.hospitality.order.model.OrderStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory reactive repository for orders.
 * In production, this would be backed by a reactive database (R2DBC, MongoDB Reactive, etc.).
 */
@Repository
public class OrderRepository {

    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();

    public Mono<Order> save(Order order) {
        orderStore.put(order.getOrderId(), order);
        return Mono.just(order);
    }

    public Mono<Order> findById(String orderId) {
        return Mono.justOrEmpty(orderStore.get(orderId));
    }

    public Flux<Order> findAll() {
        return Flux.fromIterable(orderStore.values());
    }

    public Flux<Order> findByTravelerId(String travelerId) {
        return Flux.fromIterable(orderStore.values())
                .filter(order -> order.getTravelerId().equals(travelerId));
    }

    public Flux<Order> findByStatus(OrderStatus status) {
        return Flux.fromIterable(orderStore.values())
                .filter(order -> order.getStatus() == status);
    }

    public Flux<Order> findByTravelerIdAndStatus(String travelerId, OrderStatus status) {
        return Flux.fromIterable(orderStore.values())
                .filter(order -> order.getTravelerId().equals(travelerId) && order.getStatus() == status);
    }

    public Mono<Void> deleteById(String orderId) {
        orderStore.remove(orderId);
        return Mono.empty();
    }
}
