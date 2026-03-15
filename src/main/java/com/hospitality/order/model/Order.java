package com.hospitality.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core domain model representing a hospitality order
 * following the Traveler MMF specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderId;
    private String travelerId;
    private String travelerName;
    private String propertyId;
    private String roomNumber;
    private OrderType orderType;
    private OrderStatus status;
    private SagaStatus sagaStatus;
    private List<OrderItem> items;
    private double totalAmount;
    private String specialInstructions;
    private List<DietaryPreference> dietaryPreferences;
    private LocalDateTime scheduledTime;
    private OrderPriority priority;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void calculateTotalAmount() {
        if (items != null) {
            this.totalAmount = items.stream()
                    .peek(OrderItem::calculateSubtotal)
                    .mapToDouble(OrderItem::getSubtotal)
                    .sum();
        }
    }
}
