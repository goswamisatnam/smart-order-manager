package com.hospitality.order.dto;

import com.hospitality.order.model.OrderPriority;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for modifying an existing hospitality order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyOrderRequest {

    @Valid
    private List<OrderItemRequest> items;

    private String specialInstructions;

    private LocalDateTime scheduledTime;

    private String roomNumber;

    private OrderPriority priority;
}
