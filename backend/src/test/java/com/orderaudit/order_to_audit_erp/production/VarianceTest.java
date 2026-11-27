package com.orderaudit.order_to_audit_erp.production;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class VarianceTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("3.00");

    private BigDecimal pct(String actual, String planned) {
        return Variance.exactPct(new BigDecimal(actual), new BigDecimal(planned));
    }

    @Test
    void oilOverBy500mlIsPlusFivePercentAndFlagged() {
        BigDecimal exact = pct("10.500", "10.000");
        assertThat(Variance.display(exact)).isEqualByComparingTo("5.00");
        assertThat(Variance.exceeds(exact, TOLERANCE)).isTrue();
    }

    @Test
    void yieldOf188Against200IsMinusSixPercentAndFlagged() {
        BigDecimal exact = pct("188", "200");
        assertThat(Variance.display(exact)).isEqualByComparingTo("-6.00");
        assertThat(Variance.exceeds(exact, TOLERANCE)).isTrue();
    }

    @Test
    void pouchesAtExactlyMinusThreePercentAreNotFlagged() {
        BigDecimal exact = pct("194.000", "200.000");
        assertThat(Variance.display(exact)).isEqualByComparingTo("-3.00");
        assertThat(Variance.exceeds(exact, TOLERANCE)).isFalse();
    }

    @Test
    void cartonsAtMinusTenPercentAreFlagged() {
        BigDecimal exact = pct("9.000", "10.000");
        assertThat(Variance.display(exact)).isEqualByComparingTo("-10.00");
        assertThat(Variance.exceeds(exact, TOLERANCE)).isTrue();
    }

    @Test
    void exactMatchIsZeroAndNotFlagged() {
        BigDecimal exact = pct("40.000", "40.000");
        assertThat(Variance.display(exact)).isEqualByComparingTo("0.00");
        assertThat(Variance.exceeds(exact, TOLERANCE)).isFalse();
    }

    @Test
    void valueThatDisplaysAsThreePercentButIsBeyondToleranceIsStillFlagged() {
        BigDecimal exact = pct("103.004", "100.000"); // 3.004 percent
        assertThat(Variance.display(exact)).isEqualByComparingTo("3.00");
        assertThat(Variance.exceeds(exact, TOLERANCE)).isTrue();
    }
}
