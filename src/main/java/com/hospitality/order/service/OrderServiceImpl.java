package com.hospitality.order.service;

import com.hospitality.order.dto.CancelOrderRequest;
import com.hospitality.order.dto.CreateOrderRequest;
import com.hospitality.order.dto.ModifyOrderRequest;
import com.hospitality.order.dto.OrderResponse;
import com.hospitality.order.exception.OrderNotFoundException;
import com.hospitality.order.exception.OrderStateException;
import com.hospitality.order.model.Order;
import com.hospitality.order.model.OrderItem;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.SagaStatus;
import com.hospitality.order.repository.OrderRepository;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Implementation of OrderService that coordinates with Apache Camel Saga orchestrator.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final Set<OrderStatus> MODIFIABLE_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED
    );

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING
    );

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProducerTemplate producerTemplate;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderMapper orderMapper,
                            ProducerTemplate producerTemplate) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.producerTemplate = producerTemplate;
    }

    @Override
    public Mono<OrderResponse> createOrder(CreateOrderRequest request) {
        log.info("Creating order for traveler: {} at property: {}", request.getTravelerId(), request.getPropertyId());

        Order order = orderMapper.toOrder(request);
        return orderRepository.save(order)
                .flatMap(savedOrder -> triggerCreateSaga(savedOrder)
                        .thenReturn(savedOrder))
                .map(orderMapper::toOrderResponse);
    }

    @Override
    public Mono<OrderResponse> modifyOrder(String orderId, ModifyOrderRequest request) {
        log.info("Modifying order: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order -> {
                    if (!MODIFIABLE_STATUSES.contains(order.getStatus())) {
                        return Mono.error(new OrderStateException(
                                "Order " + orderId + " cannot be modified in status: " + order.getStatus()));
                    }
                    applyModifications(order, request);
                    return orderRepository.save(order);
                })
                .flatMap(updatedOrder -> triggerModifySaga(updatedOrder)
                        .thenReturn(updatedOrder))
                .map(orderMapper::toOrderResponse);
    }

    @Override
    public Mono<OrderResponse> cancelOrder(String orderId, CancelOrderRequest request) {
        log.info("Cancelling order: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order -> {
                    if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
                        return Mono.error(new OrderStateException(
                                "Order " + orderId + " cannot be cancelled in status: " + order.getStatus()));
                    }
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setSagaStatus(SagaStatus.COMPENSATING);
                    order.setCancellationReason(request != null ? request.getReason() : null);
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .flatMap(cancelledOrder -> triggerCancelSaga(cancelledOrder)
                        .thenReturn(cancelledOrder))
                .map(orderMapper::toOrderResponse);
    }

    @Override
    public Mono<OrderResponse> getOrderById(String orderId) {
        log.info("Retrieving order: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .map(orderMapper::toOrderResponse);
    }

    @Override
    public Flux<OrderResponse> getAllOrders(String travelerId, OrderStatus status) {
        log.info("Retrieving orders - travelerId: {}, status: {}", travelerId, status);

        Flux<Order> orders;
        if (travelerId != null && status != null) {
            orders = orderRepository.findByTravelerIdAndStatus(travelerId, status);
        } else if (travelerId != null) {
            orders = orderRepository.findByTravelerId(travelerId);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }
        return orders.map(orderMapper::toOrderResponse);
    }

    private void applyModifications(Order order, ModifyOrderRequest request) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<OrderItem> updatedItems = request.getItems().stream()
                    .map(orderMapper::toOrderItem)
                    .toList();
            order.setItems(updatedItems);
            order.calculateTotalAmount();
        }
        if (request.getSpecialInstructions() != null) {
            order.setSpecialInstructions(request.getSpecialInstructions());
        }
        if (request.getScheduledTime() != null) {
            order.setScheduledTime(request.getScheduledTime());
        }
        if (request.getRoomNumber() != null) {
            order.setRoomNumber(request.getRoomNumber());
        }
        if (request.getPriority() != null) {
            order.setPriority(request.getPriority());
        }
        order.setStatus(OrderStatus.MODIFIED);
        order.setSagaStatus(SagaStatus.STARTED);
        order.setUpdatedAt(LocalDateTime.now());
    }

    private Mono<Void> triggerCreateSaga(Order order) {
        return Mono.fromRunnable(() -> {
            log.info("Triggering CREATE saga for order: {}", order.getOrderId());
            producerTemplate.asyncSendBody("direct:createOrderSaga", order);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Void> triggerModifySaga(Order order) {
        return Mono.fromRunnable(() -> {
            log.info("Triggering MODIFY saga for order: {}", order.getOrderId());
            producerTemplate.asyncSendBody("direct:modifyOrderSaga", order);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Void> triggerCancelSaga(Order order) {
        return Mono.fromRunnable(() -> {
            log.info("Triggering CANCEL saga for order: {}", order.getOrderId());
            producerTemplate.asyncSendBody("direct:cancelOrderSaga", order);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
