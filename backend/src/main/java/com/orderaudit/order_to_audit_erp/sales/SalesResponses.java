package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Small response shapes for the sales endpoints. */
public final class SalesResponses {

    private SalesResponses() {
    }

    /** One finished product on an order: how many were asked for, how many are in stock. */
    public record StockCheckLine(
            Long itemId,
            String itemCode,
            String itemName,
            int requestedQty,
            BigDecimal availableQty,
            BigDecimal shortageQty) {
    }

    public record CustomerSales(
            Long customerId,
            String customerName,
            long orderCount,
            BigDecimal totalValue) {   // pre-tax value of non-rejected, non-cancelled orders
    }

    public record DispatchResult(
            Long id,
            Long invoiceId,
            String invoiceNo,
            LocalDate dispatchDate,
            OrderStatus orderStatus) {
    }
}
