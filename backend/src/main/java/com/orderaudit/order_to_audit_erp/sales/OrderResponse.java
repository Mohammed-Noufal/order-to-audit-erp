package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        OrderStatus status,
        Long customerId,
        String customerName,
        Long salesmanId,
        String note,
        Instant createdAt,
        BigDecimal subtotal,
        List<Line> lines) {

    public record Line(
            Long id,
            Long itemId,
            String itemCode,
            String itemName,
            Integer orderedQty,
            Integer approvedQty,
            BigDecimal unitPrice,
            BigDecimal lineValue) {
    }

    public static OrderResponse from(SalesOrder order) {
        List<Line> lines = order.getLines().stream()
                .map(l -> new Line(
                        l.getId(),
                        l.getItem().getId(),
                        l.getItem().getCode(),
                        l.getItem().getName(),
                        l.getOrderedQty(),
                        l.getApprovedQty(),
                        l.getUnitPrice(),
                        l.lineValue()))
                .toList();
        BigDecimal subtotal = lines.stream()
                .map(Line::lineValue)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new OrderResponse(
                order.getId(), order.getOrderNo(), order.getStatus(),
                order.getCustomer().getId(), order.getCustomer().getName(),
                order.getSalesmanId(), order.getNote(), order.getCreatedAt(), subtotal, lines);
    }
}
