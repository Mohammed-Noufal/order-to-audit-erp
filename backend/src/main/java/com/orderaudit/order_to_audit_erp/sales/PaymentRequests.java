package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request bodies for the payment endpoints. */
public final class PaymentRequests {

    private PaymentRequests() {
    }

    public record CreatePayment(
            @NotNull Long invoiceId,
            // at most 2 decimals: a request like 100.005 is rejected, never silently rounded
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull PaymentMode mode,
            LocalDate paidOn,                       // defaults to today
            @Size(max = 60) String reference) {
    }
}
