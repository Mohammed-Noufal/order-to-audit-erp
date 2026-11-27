package com.orderaudit.order_to_audit_erp.production;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.orderaudit.order_to_audit_erp.stock.Unit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request bodies for the production endpoints. */
public final class ProductionRequests {

    private ProductionRequests() {
    }

    public record CreateRun(
            @NotNull Long productId,
            @NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal batches,
            LocalDate runDate,                       // defaults to today
            @Size(max = 30) String batchNo) {
    }

    /** qty is in the given unit, e.g. {itemId: 2, qty: 10.5, unit: L} */
    public record QtyLine(
            @NotNull Long itemId,
            @NotNull @Positive @Digits(integer = 11, fraction = 3) BigDecimal qty,
            @NotNull Unit unit) {
    }

    public record Issue(@NotEmpty @Valid List<QtyLine> lines) {
    }

    public record Complete(
            @NotNull @PositiveOrZero @Max(1_000_000) Integer actualOutput,
            @NotNull @PositiveOrZero @Max(1_000_000) Integer wastagePcs,
            @Valid List<QtyLine> packagingUsed) {
    }

    public record Overhead(
            @NotNull OverheadType type,
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 200) String note) {
    }
}
