package com.hospitality.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an individual item within a hospitality order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private String itemId;
    private String itemName;
    private int quantity;
    private double unitPrice;
    private double subtotal;
    private String customizations;

    public void calculateSubtotal() {
        this.subtotal = this.quantity * this.unitPrice;
    }
}
