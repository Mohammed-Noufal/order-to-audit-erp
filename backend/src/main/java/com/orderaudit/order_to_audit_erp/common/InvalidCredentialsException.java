package com.orderaudit.order_to_audit_erp.common;

/** Same message for wrong password, unknown user and inactive user, so attackers learn nothing. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}