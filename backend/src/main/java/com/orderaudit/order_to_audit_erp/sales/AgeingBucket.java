package com.orderaudit.order_to_audit_erp.sales;

/** How old an unpaid invoice is, counted in days from the invoice date. */
public enum AgeingBucket {
    DAYS_0_30("0-30"),
    DAYS_31_60("31-60"),
    DAYS_61_90("61-90"),
    DAYS_90_PLUS("90+");

    private final String label;

    AgeingBucket(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AgeingBucket forDays(long ageDays) {
        if (ageDays <= 30) {
            return DAYS_0_30;
        }
        if (ageDays <= 60) {
            return DAYS_31_60;
        }
        if (ageDays <= 90) {
            return DAYS_61_90;
        }
        return DAYS_90_PLUS;
    }
}
