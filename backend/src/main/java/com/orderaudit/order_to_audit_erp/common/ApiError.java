package com.orderaudit.order_to_audit_erp.common;

import java.time.Instant;
import java.util.List;

/** One JSON shape for every error the API returns. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details) {
}