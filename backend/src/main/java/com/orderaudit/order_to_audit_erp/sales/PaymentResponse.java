package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long invoiceId,
        String invoiceNo,
        String customerName,
        BigDecimal amount,
        LocalDate paidOn,
        PaymentMode mode,
        String reference,
        Long receivedBy) {

    public static PaymentResponse from(Payment payment) {
        Invoice invoice = payment.getInvoice();
        return new PaymentResponse(
                payment.getId(),
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getCustomer().getName(),
                payment.getAmount(),
                payment.getPaidOn(),
                payment.getMode(),
                payment.getReference(),
                payment.getReceivedBy());
    }
}
