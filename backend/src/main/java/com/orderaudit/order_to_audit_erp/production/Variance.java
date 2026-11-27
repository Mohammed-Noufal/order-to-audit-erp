package com.orderaudit.order_to_audit_erp.production;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Variance {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Variance() {
    }

    /** (actual - planned) / planned x 100, kept at 6 decimals for comparison. planned must be > 0. */
    public static BigDecimal exactPct(BigDecimal actual, BigDecimal planned) {
        return actual.subtract(planned).multiply(HUNDRED).divide(planned, 6, RoundingMode.HALF_UP);
    }

    /** The 2-decimal value shown to people and stored. */
    public static BigDecimal display(BigDecimal exactPct) {
        return exactPct.setScale(2, RoundingMode.HALF_UP);
    }

    /** Flagged only when strictly beyond the tolerance: exactly 3.00 against 3.00 is not flagged. */
    public static boolean exceeds(BigDecimal exactPct, BigDecimal tolerancePct) {
        return exactPct.abs().compareTo(tolerancePct) > 0;
    }
}
