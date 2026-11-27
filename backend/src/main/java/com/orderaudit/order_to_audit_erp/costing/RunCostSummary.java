package com.orderaudit.order_to_audit_erp.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RunCostSummary(
        BigDecimal materialCost,
        BigDecimal overheadCost,
        BigDecimal totalCost,
        BigDecimal costPerPacket,     // null until the run has output
        BigDecimal sellingPrice,
        BigDecimal marginPerPacket,   // null until the run has output
        BigDecimal marginPct) {       // null until the run has output, or if there is no price

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static RunCostSummary calculate(BigDecimal materialCost, BigDecimal overheadCost,
                                           Integer actualOutput, BigDecimal sellingPrice) {
        BigDecimal material = materialCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal overhead = overheadCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = material.add(overhead); // displayed columns always add up
        BigDecimal price = (sellingPrice != null) ? sellingPrice : BigDecimal.ZERO.setScale(2);

        if (actualOutput == null || actualOutput <= 0) {
            return new RunCostSummary(material, overhead, total, null, price, null, null);
        }

        // Exact cost per good packet: whole run cost (damaged pouches included) / good packets
        BigDecimal exactCost = materialCost.add(overheadCost)
                .divide(BigDecimal.valueOf(actualOutput), 4, RoundingMode.HALF_UP);
        BigDecimal exactMargin = price.subtract(exactCost);
        BigDecimal marginPct = price.signum() > 0
                ? exactMargin.multiply(HUNDRED).divide(price, 2, RoundingMode.HALF_UP)
                : null;

        return new RunCostSummary(material, overhead, total,
                exactCost.setScale(2, RoundingMode.HALF_UP), price,
                exactMargin.setScale(2, RoundingMode.HALF_UP), marginPct);
    }
}
