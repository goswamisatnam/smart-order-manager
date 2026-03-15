package com.hospitality.order.orchestrator.routes;

import com.hospitality.order.model.Order;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.SagaStatus;
import com.hospitality.order.repository.OrderRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.SagaPropagation;
import org.apache.camel.saga.InMemorySagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Apache Camel Saga route for creating orders.
 * Orchestrates: Inventory Reservation → Payment Processing → Service Notification.
 * Each step has a corresponding compensation action for rollback.
 */
@Component
public class CreateOrderSagaRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderSagaRoute.class);

    private final OrderRepository orderRepository;

    public CreateOrderSagaRoute(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void configure() throws Exception {

        getContext().addService(new InMemorySagaService());

        // Main Create Order Saga
        from("direct:createOrderSaga")
                .routeId("create-order-saga")
                .log("Starting CREATE ORDER saga for order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.REQUIRES_NEW)
                                        .completion("direct:createOrderComplete")
                                        .compensation("direct:createOrderCompensate")
                .to("direct:reserveInventory")
                .to("direct:processPayment")
                .to("direct:notifyService")
                .log("CREATE ORDER saga completed successfully for order: ${body.orderId}");

        // Step 1: Reserve Inventory
        from("direct:reserveInventory")
                .routeId("reserve-inventory")
                .log("Reserving inventory for order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:releaseInventory")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    Order current = orderRepository.findById(order.getOrderId()).block();
                    if (current != null && current.getStatus() == OrderStatus.CANCELLED) {
                        log.info("Order {} already cancelled, skipping inventory reservation", order.getOrderId());
                        return;
                    }
                    log.info("Reserving inventory for {} items in order {}",
                            order.getItems().size(), order.getOrderId());
                    order.setSagaStatus(SagaStatus.INVENTORY_RESERVED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Inventory reserved for order: ${body.orderId}");

        // Step 1 Compensation: Release Inventory
        from("direct:releaseInventory")
                .routeId("release-inventory")
                .log("COMPENSATING: Releasing inventory for order")
                .process(exchange -> {
                    log.warn("Releasing reserved inventory due to saga compensation");
                });

        // Step 2: Process Payment
        from("direct:processPayment")
                .routeId("process-payment")
                .log("Processing payment for order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:refundPayment")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    Order current = orderRepository.findById(order.getOrderId()).block();
                    if (current != null && current.getStatus() == OrderStatus.CANCELLED) {
                        log.info("Order {} already cancelled, skipping payment processing", order.getOrderId());
                        return;
                    }
                    log.info("Processing payment of {} for order {}",
                            order.getTotalAmount(), order.getOrderId());
                    order.setSagaStatus(SagaStatus.PAYMENT_PROCESSED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Payment processed for order: ${body.orderId}");

        // Step 2 Compensation: Refund Payment
        from("direct:refundPayment")
                .routeId("refund-payment")
                .log("COMPENSATING: Refunding payment for order")
                .process(exchange -> {
                    log.warn("Refunding payment due to saga compensation");
                });

        // Step 3: Notify Service (Kitchen/Housekeeping/Spa/etc.)
        from("direct:notifyService")
                .routeId("notify-service")
                .log("Notifying service for order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:cancelServiceNotification")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    Order current = orderRepository.findById(order.getOrderId()).block();
                    if (current != null && current.getStatus() == OrderStatus.CANCELLED) {
                        log.info("Order {} already cancelled, skipping service notification", order.getOrderId());
                        return;
                    }
                    log.info("Notifying {} service for order {}",
                            order.getOrderType(), order.getOrderId());
                    order.setSagaStatus(SagaStatus.SERVICE_NOTIFIED);
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Service notified for order: ${body.orderId}");

        // Step 3 Compensation: Cancel Service Notification
        from("direct:cancelServiceNotification")
                .routeId("cancel-service-notification")
                .log("COMPENSATING: Cancelling service notification for order")
                .process(exchange -> {
                    log.warn("Cancelling service notification due to saga compensation");
                });

        // Saga Completion Handler
        from("direct:createOrderComplete")
                .routeId("create-order-complete")
                .log("CREATE ORDER saga COMPLETED successfully")
                .process(exchange -> {
                    log.info("Order creation saga completed - all steps successful");
                });

        // Saga Compensation Handler (Global)
        from("direct:createOrderCompensate")
                .routeId("create-order-compensate")
                .log("CREATE ORDER saga COMPENSATING - rolling back all steps")
                .process(exchange -> {
                    log.error("Order creation saga failed - executing compensation");
                });
    }
}
