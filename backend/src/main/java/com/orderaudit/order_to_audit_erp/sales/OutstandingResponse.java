package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OutstandingResponse(
        Long customerId,
        String customerName,
        String gstin,
        LocalDate asOfDate,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalOutstanding,
        Ageing ageing,
        List<InvoiceRow> invoices) {

    /** Unpaid balance split by invoice age. The four buckets add up to totalOutstanding. */
    public record Ageing(
            BigDecimal days0To30,
            BigDecimal days31To60,
            BigDecimal days61To90,
            BigDecimal days90Plus) {
    }

    public record InvoiceRow(
            Long invoiceId,
            String invoiceNo,
            LocalDate invoiceDate,
            InvoiceStatus status,
            BigDecimal invoiceTotal,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            long ageDays,
            String ageingBucket) {
    }
}
