package com.orderaudit.order_to_audit_erp.purchase;

import java.math.BigDecimal;

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
@Table(name = "purchase_lines")
public class PurchaseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Both stored in the item's stock unit
    @Column(name = "qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal qty;

    @Column(name = "rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal rate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    public Long getId() { return id; }
    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
