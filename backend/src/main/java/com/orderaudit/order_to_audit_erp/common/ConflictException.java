package com.orderaudit.order_to_audit_erp.common;

/** A business-rule conflict, such as insufficient stock (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
