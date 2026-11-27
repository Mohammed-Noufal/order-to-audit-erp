package com.orderaudit.order_to_audit_erp.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String username,
        Role role) {
}