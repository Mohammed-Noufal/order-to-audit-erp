package com.orderaudit.order_to_audit_erp.master;

import java.math.BigDecimal;

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
@Table(name = "bom_lines")
public class BomLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // In the item's stock unit (kg, L or pcs)
    @Column(name = "qty_per_batch", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyPerBatch;

    public Long getId() { return id; }
    public Bom getBom() { return bom; }
    public void setBom(Bom bom) { this.bom = bom; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public BigDecimal getQtyPerBatch() { return qtyPerBatch; }
    public void setQtyPerBatch(BigDecimal qtyPerBatch) { this.qtyPerBatch = qtyPerBatch; }
}
