package com.orderaudit.order_to_audit_erp.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.orderaudit.order_to_audit_erp.stock.Unit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePurchaseRequest(
        @NotNull Long supplierId,
        LocalDate purchaseDate,                 // defaults to today
        @Size(max = 40) String supplierBillNo,
        @NotEmpty @Valid List<Line> lines) {

    /** qty and rate are in the unit given, e.g. {itemId: 1, qty: 60, unit: KG, rate: 45} */
    public record Line(
            @NotNull Long itemId,
            @NotNull @Positive @Digits(integer = 11, fraction = 3) BigDecimal qty,
            @NotNull Unit unit,
            @NotNull @Positive @Digits(integer = 8, fraction = 4) BigDecimal rate) {
    }
}
