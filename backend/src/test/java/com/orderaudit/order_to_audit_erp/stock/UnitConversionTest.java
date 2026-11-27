package com.orderaudit.order_to_audit_erp.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.StockUnit;

class UnitConversionTest {

    private Item itemStoredIn(StockUnit unit) {
        Item item = new Item();
        item.setCode("TEST-ITEM");
        item.setStockUnit(unit);
        return item;
    }

    @Test
    void millilitresConvertToLitres() {
        assertThat(Unit.ML.toStockQty(new BigDecimal("10500"), itemStoredIn(StockUnit.L)))
                .isEqualByComparingTo("10.500");
    }

    @Test
    void gramsConvertToKilograms() {
        assertThat(Unit.G.toStockQty(new BigDecimal("1200"), itemStoredIn(StockUnit.KG)))
                .isEqualByComparingTo("1.200");
    }

    @Test
    void sameUnitIsUnchanged() {
        assertThat(Unit.L.toStockQty(new BigDecimal("10.5"), itemStoredIn(StockUnit.L)))
                .isEqualByComparingTo("10.500");
    }

    @Test
    void packetsAndPiecesAreInterchangeable() {
        assertThat(Unit.PACKET.toStockQty(new BigDecimal("188"), itemStoredIn(StockUnit.PCS)))
                .isEqualByComparingTo("188.000");
    }

    @Test
    void massCannotConvertToVolume() {
        assertThatThrownBy(() -> Unit.KG.toStockQty(BigDecimal.ONE, itemStoredIn(StockUnit.L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unit mismatch");
    }

    @Test
    void tinyQuantityRoundsToZeroSoCallersMustCheck() {
        assertThat(Unit.G.toStockQty(new BigDecimal("0.4"), itemStoredIn(StockUnit.KG)).signum())
                .isZero();
    }

    @Test
    void ratePerGramConvertsToRatePerKilogram() {
        assertThat(Unit.G.toStockRate(new BigDecimal("0.045"), itemStoredIn(StockUnit.KG)))
                .isEqualByComparingTo("45.0000");
    }

    @Test
    void ratePerMillilitreConvertsToRatePerLitre() {
        assertThat(Unit.ML.toStockRate(new BigDecimal("0.14"), itemStoredIn(StockUnit.L)))
                .isEqualByComparingTo("140.0000");
    }
}
