package com.hospitality.order.dto;

import com.hospitality.order.model.DietaryPreference;
import com.hospitality.order.model.OrderPriority;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.OrderType;
import com.hospitality.order.model.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a hospitality order with saga status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;
    private String travelerId;
    private String travelerName;
    private String propertyId;
    private String roomNumber;
    private OrderType orderType;
    private OrderStatus status;
    private SagaStatus sagaStatus;
    private List<OrderItemResponse> items;
    private double totalAmount;
    private String specialInstructions;
    private List<DietaryPreference> dietaryPreferences;
    private LocalDateTime scheduledTime;
    private OrderPriority priority;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
