package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure GST arithmetic, no database. All amounts are rupees with 2 decimals, HALF_UP. */
public final class GstCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private GstCalculator() {
    }

    public record LineTax(
            BigDecimal taxableValue,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal lineTotal) {
    }

    public static LineTax calculate(int qty, BigDecimal rate, BigDecimal gstRatePct, boolean intraState) {
        BigDecimal taxable = rate.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTax = taxable.multiply(gstRatePct).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        BigDecimal cgst = zero;
        BigDecimal sgst = zero;
        BigDecimal igst = zero;

        if (intraState) {
            cgst = totalTax.divide(TWO, 2, RoundingMode.HALF_UP);
            sgst = totalTax.subtract(cgst); // never rounds separately, so cgst + sgst == totalTax
        } else {
            igst = totalTax;
        }

        BigDecimal lineTotal = taxable.add(cgst).add(sgst).add(igst);
        return new LineTax(taxable, cgst, sgst, igst, lineTotal);
    }
}
