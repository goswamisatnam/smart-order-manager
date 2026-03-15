package com.hospitality.order.model;

/**
 * Lifecycle status of an order.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    MODIFIED,
    CANCELLED,
    FAILED
}
