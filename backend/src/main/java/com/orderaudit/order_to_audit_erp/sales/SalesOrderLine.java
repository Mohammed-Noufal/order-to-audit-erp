package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.orderaudit.order_to_audit_erp.master.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    // What the admin approved (may differ from what was ordered). Null until approval or edit.
    @Column(name = "approved_qty")
    private Integer approvedQty;

    // Price snapshot taken when the order was placed (admin may edit it)
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public SalesOrder getSalesOrder() { return salesOrder; }
    public void setSalesOrder(SalesOrder salesOrder) { this.salesOrder = salesOrder; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Integer getOrderedQty() { return orderedQty; }
    public void setOrderedQty(Integer orderedQty) { this.orderedQty = orderedQty; }
    public Integer getApprovedQty() { return approvedQty; }
    public void setApprovedQty(Integer approvedQty) { this.approvedQty = approvedQty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    /** The quantity that counts: approved if the admin set it, otherwise what was ordered. */
    public int effectiveQty() {
        return (approvedQty != null) ? approvedQty : orderedQty;
    }

    /** Pre-tax value of this line. */
    public BigDecimal lineValue() {
        return unitPrice.multiply(BigDecimal.valueOf(effectiveQty())).setScale(2, RoundingMode.HALF_UP);
    }
}
