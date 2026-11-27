package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;

public enum MovementType {
    OPENING(1),
    PURCHASE(1),
    PROD_ISSUE(-1),
    PROD_OUTPUT(1),
    SALE_DISPATCH(-1),
    SALE_RETURN(1),
    ADJUSTMENT(0); // either direction

    private final int sign;

    MovementType(int sign) {
        this.sign = sign;
    }

    public boolean allows(BigDecimal qtyDelta) {
        return sign == 0 || qtyDelta.signum() == sign;
    }
}
