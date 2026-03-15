package com.hospitality.order.orchestrator.routes;

import com.hospitality.order.model.Order;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.SagaStatus;
import com.hospitality.order.repository.OrderRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.SagaPropagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Apache Camel Saga route for modifying orders.
 * Orchestrates: Inventory Adjustment → Payment Recalculation → Service Update.
 * Each step has a corresponding compensation action for rollback.
 */
@Component
public class ModifyOrderSagaRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(ModifyOrderSagaRoute.class);

    private final OrderRepository orderRepository;

    public ModifyOrderSagaRoute(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void configure() throws Exception {

        // Main Modify Order Saga
        from("direct:modifyOrderSaga")
                .routeId("modify-order-saga")
                .log("Starting MODIFY ORDER saga for order: ${body.orderId}")
                .saga()
                    .propagation(SagaPropagation.REQUIRES_NEW)
                    .completion("direct:modifyOrderComplete")
                    .compensation("direct:modifyOrderCompensate")
                .to("direct:adjustInventory")
                .to("direct:recalculatePayment")
                .to("direct:updateServiceNotification")
                .log("MODIFY ORDER saga completed successfully for order: ${body.orderId}");

        // Step 1: Adjust Inventory
        from("direct:adjustInventory")
                .routeId("adjust-inventory")
                .log("Adjusting inventory for modified order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:revertInventoryAdjustment")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    log.info("Adjusting inventory for modified order {}", order.getOrderId());
                    order.setSagaStatus(SagaStatus.INVENTORY_RESERVED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Inventory adjusted for order: ${body.orderId}");

        // Step 1 Compensation: Revert Inventory Adjustment
        from("direct:revertInventoryAdjustment")
                .routeId("revert-inventory-adjustment")
                .log("COMPENSATING: Reverting inventory adjustment")
                .process(exchange -> {
                    log.warn("Reverting inventory adjustment due to modify saga compensation");
                });

        // Step 2: Recalculate Payment
        from("direct:recalculatePayment")
                .routeId("recalculate-payment")
                .log("Recalculating payment for modified order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:revertPaymentRecalculation")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    log.info("Recalculating payment for order {} - new total: {}",
                            order.getOrderId(), order.getTotalAmount());
                    order.setSagaStatus(SagaStatus.PAYMENT_PROCESSED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Payment recalculated for order: ${body.orderId}");

        // Step 2 Compensation: Revert Payment Recalculation
        from("direct:revertPaymentRecalculation")
                .routeId("revert-payment-recalculation")
                .log("COMPENSATING: Reverting payment recalculation")
                .process(exchange -> {
                    log.warn("Reverting payment recalculation due to modify saga compensation");
                });

        // Step 3: Update Service Notification
        from("direct:updateServiceNotification")
                .routeId("update-service-notification")
                .log("Updating service notification for modified order: ${body.orderId}")
                .saga()
                                        .propagation(SagaPropagation.MANDATORY)
                                        .compensation("direct:revertServiceUpdate")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    log.info("Updating {} service for modified order {}",
                            order.getOrderType(), order.getOrderId());
                    order.setSagaStatus(SagaStatus.COMPLETED);
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order).block();
                })
                .log("Service updated for order: ${body.orderId}");

        // Step 3 Compensation: Revert Service Update
        from("direct:revertServiceUpdate")
                .routeId("revert-service-update")
                .log("COMPENSATING: Reverting service update notification")
                .process(exchange -> {
                    log.warn("Reverting service update notification due to modify saga compensation");
                });

        // Saga Completion Handler
        from("direct:modifyOrderComplete")
                .routeId("modify-order-complete")
                .log("MODIFY ORDER saga COMPLETED successfully")
                .process(exchange -> {
                    log.info("Order modification saga completed - all steps successful");
                });

        // Saga Compensation Handler (Global)
        from("direct:modifyOrderCompensate")
                .routeId("modify-order-compensate")
                .log("MODIFY ORDER saga COMPENSATING - rolling back all steps")
                .process(exchange -> {
                    log.error("Order modification saga failed - executing compensation");
                });
    }
}
