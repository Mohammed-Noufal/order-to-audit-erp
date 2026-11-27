package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;

public record StockBalanceResponse(
        Long itemId,
        String code,
        String name,
        String itemType,
        String stockUnit,
        BigDecimal balance,
        BigDecimal avgCost,
        BigDecimal reorderLevel) {

    public static StockBalanceResponse from(StockBalanceView v) {
        return new StockBalanceResponse(v.getItemId(), v.getCode(), v.getName(), v.getItemType(),
                v.getStockUnit(), v.getBalance(), v.getAvgCost(), v.getReorderLevel());
    }
}
