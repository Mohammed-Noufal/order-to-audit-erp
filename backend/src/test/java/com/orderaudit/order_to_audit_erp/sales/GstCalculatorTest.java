package com.orderaudit.order_to_audit_erp.sales;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GstCalculatorTest {

    private static final BigDecimal FIVE_PERCENT = new BigDecimal("5.00");

    @Test
    void retailerAInTamilNaduGetsCgstAndSgst() {
        GstCalculator.LineTax tax = GstCalculator.calculate(150, new BigDecimal("50.00"), FIVE_PERCENT, true);

        assertThat(tax.taxableValue()).isEqualByComparingTo("7500.00");
        assertThat(tax.cgst()).isEqualByComparingTo("187.50");
        assertThat(tax.sgst()).isEqualByComparingTo("187.50");
        assertThat(tax.igst()).isEqualByComparingTo("0.00");
        assertThat(tax.lineTotal()).isEqualByComparingTo("7875.00");
    }

    @Test
    void distributorBInKarnatakaGetsIgstOnly() {
        GstCalculator.LineTax tax = GstCalculator.calculate(120, new BigDecimal("50.00"), FIVE_PERCENT, false);

        assertThat(tax.taxableValue()).isEqualByComparingTo("6000.00");
        assertThat(tax.cgst()).isEqualByComparingTo("0.00");
        assertThat(tax.sgst()).isEqualByComparingTo("0.00");
        assertThat(tax.igst()).isEqualByComparingTo("300.00");
        assertThat(tax.lineTotal()).isEqualByComparingTo("6300.00");
    }

    @Test
    void reducedQuantityOf88PacketsToKarnatakaGivesIgstOf220() {
        GstCalculator.LineTax tax = GstCalculator.calculate(88, new BigDecimal("50.00"), FIVE_PERCENT, false);

        assertThat(tax.taxableValue()).isEqualByComparingTo("4400.00");
        assertThat(tax.igst()).isEqualByComparingTo("220.00");
    }

    @Test
    void oddPaisaTaxSplitsSoTheHalvesAddBackToTheExactTax() {
        // 33.33 at 5% = 1.6665 -> 1.67 total tax. Halves: 0.84 + 0.83 = 1.67
        GstCalculator.LineTax tax = GstCalculator.calculate(1, new BigDecimal("33.33"), FIVE_PERCENT, true);

        assertThat(tax.cgst().add(tax.sgst())).isEqualByComparingTo("1.67");
        assertThat(tax.cgst()).isEqualByComparingTo("0.84");
        assertThat(tax.sgst()).isEqualByComparingTo("0.83");
        assertThat(tax.lineTotal()).isEqualByComparingTo("35.00");
    }

    @Test
    void zeroRatedGoodsHaveNoTax() {
        GstCalculator.LineTax tax = GstCalculator.calculate(10, new BigDecimal("20.00"), BigDecimal.ZERO, true);

        assertThat(tax.cgst()).isEqualByComparingTo("0.00");
        assertThat(tax.sgst()).isEqualByComparingTo("0.00");
        assertThat(tax.lineTotal()).isEqualByComparingTo("200.00");
    }
}
