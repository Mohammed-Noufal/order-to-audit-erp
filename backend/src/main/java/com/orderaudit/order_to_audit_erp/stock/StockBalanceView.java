package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;

/** Projection for the balance query. Getter names must match the SQL aliases. */
public interface StockBalanceView {
    Long getItemId();
    String getCode();
    String getName();
    String getItemType();
    String getStockUnit();
    BigDecimal getBalance();
    BigDecimal getAvgCost();
    BigDecimal getReorderLevel();
}
