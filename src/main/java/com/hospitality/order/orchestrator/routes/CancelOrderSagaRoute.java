package com.hospitality.order.orchestrator.routes;

import com.hospitality.order.model.Order;
import com.hospitality.order.model.SagaStatus;
import com.hospitality.order.repository.OrderRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.SagaPropagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Apache Camel Saga route for cancelling orders.
 * Orchestrates: Service Cancellation → Payment Refund → Inventory Release.
 * Performs compensating transactions in reverse order of creation.
 */
@Component
public class CancelOrderSagaRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderSagaRoute.class);

    private final OrderRepository orderRepository;

    public CancelOrderSagaRoute(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void configure() throws Exception {

        // Main Cancel Order Saga
        from("direct:cancelOrderSaga")
                .routeId("cancel-order-saga")
                .log("Starting CANCEL ORDER saga for order: ${body.orderId}")
                .saga()
                    .propagation(SagaPropagation.REQUIRES_NEW)
                    .completion("direct:cancelOrderComplete")
                    .compensation("direct:cancelOrderCompensate")
                .to("direct:cancelServiceOrder")
                .to("direct:processRefund")
                .to("direct:releaseOrderInventory")
                .log("CANCEL ORDER saga completed successfully for order: ${body.orderId}");

        // Step 1: Cancel Service Order (Kitchen/Housekeeping/Spa/etc.)
        from("direct:cancelServiceOrder")
                .routeId("cancel-service-order")
                .log("Cancelling service order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:reinstateServiceOrder")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    log.info("Cancelling {} service for order {}",
                            order.getOrderType(), order.getOrderId());
                    order.setSagaStatus(SagaStatus.COMPENSATING);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Service order cancelled for: ${body.orderId}");

        // Step 1 Compensation: Reinstate Service Order
        from("direct:reinstateServiceOrder")
                .routeId("reinstate-service-order")
                .log("COMPENSATING: Reinstating service order")
                .process(exchange -> {
                    log.warn("Reinstating service order due to cancel saga compensation failure");
                });

        // Step 2: Process Refund
        from("direct:processRefund")
                .routeId("process-refund")
                .log("Processing refund for order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:reverseRefund")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    log.info("Processing refund of {} for order {}",
                            order.getTotalAmount(), order.getOrderId());
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Refund processed for order: ${body.orderId}");

        // Step 2 Compensation: Reverse Refund
        from("direct:reverseRefund")
                .routeId("reverse-refund")
                .log("COMPENSATING: Reversing refund")
                .process(exchange -> {
                    log.warn("Reversing refund due to cancel saga compensation failure");
                });

        // Step 3: Release Order Inventory
        from("direct:releaseOrderInventory")
                .routeId("release-order-inventory")
                .log("Releasing inventory for cancelled order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:reReserveInventory")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    log.info("Releasing inventory for {} items in cancelled order {}",
                            order.getItems().size(), order.getOrderId());
                    order.setSagaStatus(SagaStatus.COMPENSATED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Inventory released for order: ${body.orderId}");

        // Step 3 Compensation: Re-reserve Inventory
        from("direct:reReserveInventory")
                .routeId("re-reserve-inventory")
                .log("COMPENSATING: Re-reserving inventory")
                .process(exchange -> {
                    log.warn("Re-reserving inventory due to cancel saga compensation failure");
                });

        // Saga Completion Handler
        from("direct:cancelOrderComplete")
                .routeId("cancel-order-complete")
                .log("CANCEL ORDER saga COMPLETED successfully")
                .process(exchange -> {
                    log.info("Order cancellation saga completed - all compensating transactions successful");
                });

        // Saga Compensation Handler (Global)
        from("direct:cancelOrderCompensate")
                .routeId("cancel-order-compensate")
                .log("CANCEL ORDER saga COMPENSATING - cancellation rollback")
                .process(exchange -> {
                    log.error("Order cancellation saga failed - compensation required");
                });
    }
}
