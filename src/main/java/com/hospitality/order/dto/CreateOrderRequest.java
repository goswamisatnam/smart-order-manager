package com.hospitality.order.dto;

import com.hospitality.order.model.DietaryPreference;
import com.hospitality.order.model.OrderPriority;
import com.hospitality.order.model.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating a new hospitality order
 * following the Traveler MMF specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Traveler ID is required")
    private String travelerId;

    private String travelerName;

    @NotBlank(message = "Property ID is required")
    private String propertyId;

    private String roomNumber;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<OrderItemRequest> items;

    private String specialInstructions;

    private List<DietaryPreference> dietaryPreferences;

    private LocalDateTime scheduledTime;

    @Builder.Default
    private OrderPriority priority = OrderPriority.NORMAL;
}
