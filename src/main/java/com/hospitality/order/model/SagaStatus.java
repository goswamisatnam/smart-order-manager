package com.hospitality.order.model;

/**
 * Status tracking for the Saga orchestration lifecycle.
 */
public enum SagaStatus {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    SERVICE_NOTIFIED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
