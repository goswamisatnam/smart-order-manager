package com.hospitality.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for an individual order item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private String itemId;
    private String itemName;
    private int quantity;
    private double unitPrice;
    private double subtotal;
    private String customizations;
}
