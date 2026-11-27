package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.StockUnit;

public enum Unit {
    KG(Dimension.MASS, "1"),
    G(Dimension.MASS, "0.001"),       // 1 g = 0.001 kg
    L(Dimension.VOLUME, "1"),
    ML(Dimension.VOLUME, "0.001"),    // 1 ml = 0.001 L
    PCS(Dimension.COUNT, "1"),
    PACKET(Dimension.COUNT, "1");     // a carton is NOT a unit: it is a packaging item

    public enum Dimension { MASS, VOLUME, COUNT }

    private final Dimension dimension;
    private final BigDecimal factor; // how many base units (kg, L, pcs) one of this unit equals

    Unit(Dimension dimension, String factor) {
        this.dimension = dimension;
        this.factor = new BigDecimal(factor);
    }

    public static Unit of(StockUnit stockUnit) {
        return Unit.valueOf(stockUnit.name());
    }

    /** Quantity entered in this unit -> the item's stock unit, 3 decimals. 10500 ML on an L item = 10.500 */
    public BigDecimal toStockQty(BigDecimal qty, Item item) {
        Unit target = targetFor(item);
        return qty.multiply(this.factor).divide(target.factor, 3, RoundingMode.HALF_UP);
    }

    /** Rate per this unit -> rate per the item's stock unit, 4 decimals. Rs 0.045 per G on a KG item = 45.0000 */
    public BigDecimal toStockRate(BigDecimal ratePerThisUnit, Item item) {
        Unit target = targetFor(item);
        return ratePerThisUnit.multiply(target.factor).divide(this.factor, 4, RoundingMode.HALF_UP);
    }

    private Unit targetFor(Item item) {
        Unit target = Unit.of(item.getStockUnit());
        if (this.dimension != target.dimension) {
            throw new BadRequestException("Unit mismatch for " + item.getCode()
                    + ": cannot convert " + this + " to " + target);
        }
        return target;
    }
}
