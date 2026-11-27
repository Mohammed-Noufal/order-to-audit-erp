package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LedgerEntryResponse(
        Long id,
        Long itemId,
        String itemCode,
        String itemName,
        String stockUnit,
        LocalDate movementDate,
        MovementType movementType,
        BigDecimal qtyDelta,
        BigDecimal unitCost,
        SourceType sourceType,
        Long sourceId,
        Long createdBy,
        Instant createdAt) {

    public static LedgerEntryResponse from(LedgerEntry e) {
        return new LedgerEntryResponse(
                e.getId(),
                e.getItem().getId(),
                e.getItem().getCode(),
                e.getItem().getName(),
                e.getItem().getStockUnit().name(),
                e.getMovementDate(),
                e.getMovementType(),
                e.getQtyDelta(),
                e.getUnitCost(),
                e.getSourceType(),
                e.getSourceId(),
                e.getCreatedBy(),
                e.getCreatedAt());
    }
}
