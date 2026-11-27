package com.orderaudit.order_to_audit_erp.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseResponse(
        Long id,
        String purchaseNo,
        Long supplierId,
        String supplierName,
        LocalDate purchaseDate,
        String supplierBillNo,
        BigDecimal total,
        List<Line> lines) {

    public record Line(
            Long itemId,
            String itemCode,
            String itemName,
            String stockUnit,
            BigDecimal qty,
            BigDecimal rate,
            BigDecimal amount) {
    }

    public static PurchaseResponse from(Purchase p) {
        List<Line> lines = p.getLines().stream()
                .map(l -> new Line(
                        l.getItem().getId(),
                        l.getItem().getCode(),
                        l.getItem().getName(),
                        l.getItem().getStockUnit().name(),
                        l.getQty(),
                        l.getRate(),
                        l.getAmount()))
                .toList();
        BigDecimal total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseResponse(p.getId(), p.getPurchaseNo(), p.getSupplier().getId(),
                p.getSupplier().getName(), p.getPurchaseDate(), p.getSupplierBillNo(), total, lines);
    }
}
