package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;

import com.orderaudit.order_to_audit_erp.common.ConflictException;
import com.orderaudit.order_to_audit_erp.master.Item;

public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(Item item, BigDecimal available, BigDecimal required) {
        super("Insufficient stock for %s (%s): available %s %s, required %s %s".formatted(
                item.getName(), item.getCode(),
                available.toPlainString(), item.getStockUnit(),
                required.toPlainString(), item.getStockUnit()));
    }
}
