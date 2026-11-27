package com.orderaudit.order_to_audit_erp.common;

/** A business-rule violation caused by the caller's input (HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
