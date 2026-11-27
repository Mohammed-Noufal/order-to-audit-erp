package com.orderaudit.order_to_audit_erp.costing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RunCostSummaryTest {

    @Test
    void benchmarkRunCostsAboutThirtyOneRupeesFourteenPaisePerPacket() {
        // flour 1725 + oil 1470 + spice 240 + pouches 485 + cartons 135 = 4055 materials
        RunCostSummary summary = RunCostSummary.calculate(
                new BigDecimal("4055.0000"), new BigDecimal("1800.00"), 188, new BigDecimal("50.00"));

        assertThat(summary.materialCost()).isEqualByComparingTo("4055.00");
        assertThat(summary.overheadCost()).isEqualByComparingTo("1800.00");
        assertThat(summary.totalCost()).isEqualByComparingTo("5855.00");
        assertThat(summary.costPerPacket()).isEqualByComparingTo("31.14");
        assertThat(summary.marginPerPacket()).isEqualByComparingTo("18.86");
        assertThat(summary.marginPct()).isEqualByComparingTo("37.71");
    }

    @Test
    void costPerPacketIsNullUntilTheRunHasOutput() {
        RunCostSummary summary = RunCostSummary.calculate(
                new BigDecimal("4055.0000"), BigDecimal.ZERO, null, new BigDecimal("50.00"));

        assertThat(summary.totalCost()).isEqualByComparingTo("4055.00");
        assertThat(summary.costPerPacket()).isNull();
        assertThat(summary.marginPerPacket()).isNull();
        assertThat(summary.marginPct()).isNull();
    }

    @Test
    void zeroSellingPriceGivesNoMarginPercentage() {
        RunCostSummary summary = RunCostSummary.calculate(
                new BigDecimal("100.0000"), BigDecimal.ZERO, 10, null);

        assertThat(summary.costPerPacket()).isEqualByComparingTo("10.00");
        assertThat(summary.marginPct()).isNull();
    }
}
