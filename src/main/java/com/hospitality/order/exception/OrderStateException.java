package com.hospitality.order.exception;

/**
 * Exception thrown when an order operation is invalid for the current order state.
 */
public class OrderStateException extends RuntimeException {

    public OrderStateException(String message) {
        super(message);
    }
}
