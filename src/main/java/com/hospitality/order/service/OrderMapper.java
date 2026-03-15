package com.hospitality.order.service;

import com.hospitality.order.dto.CreateOrderRequest;
import com.hospitality.order.dto.OrderItemRequest;
import com.hospitality.order.dto.OrderItemResponse;
import com.hospitality.order.dto.OrderResponse;
import com.hospitality.order.model.Order;
import com.hospitality.order.model.OrderItem;
import com.hospitality.order.model.OrderPriority;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.SagaStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Maps between domain models and DTOs.
 */
@Component
public class OrderMapper {

    public Order toOrder(CreateOrderRequest request) {
        List<OrderItem> items = request.getItems().stream()
                .map(this::toOrderItem)
                .toList();

        Order order = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .travelerId(request.getTravelerId())
                .travelerName(request.getTravelerName())
                .propertyId(request.getPropertyId())
                .roomNumber(request.getRoomNumber())
                .orderType(request.getOrderType())
                .status(OrderStatus.PENDING)
                .sagaStatus(SagaStatus.STARTED)
                .items(items)
                .specialInstructions(request.getSpecialInstructions())
                .dietaryPreferences(request.getDietaryPreferences())
                .scheduledTime(request.getScheduledTime())
                .priority(request.getPriority() != null ? request.getPriority() : OrderPriority.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        order.calculateTotalAmount();
        return order;
    }

    public OrderItem toOrderItem(OrderItemRequest request) {
        OrderItem item = OrderItem.builder()
                .itemId(request.getItemId())
                .itemName(request.getItemName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .customizations(request.getCustomizations())
                .build();
        item.calculateSubtotal();
        return item;
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::toOrderItemResponse).toList()
                : List.of();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .travelerId(order.getTravelerId())
                .travelerName(order.getTravelerName())
                .propertyId(order.getPropertyId())
                .roomNumber(order.getRoomNumber())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .sagaStatus(order.getSagaStatus())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .specialInstructions(order.getSpecialInstructions())
                .dietaryPreferences(order.getDietaryPreferences())
                .scheduledTime(order.getScheduledTime())
                .priority(order.getPriority())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .customizations(item.getCustomizations())
                .build();
    }
}
