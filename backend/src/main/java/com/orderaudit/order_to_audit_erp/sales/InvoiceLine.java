package com.orderaudit.order_to_audit_erp.sales;

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
@Table(name = "invoice_lines")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // hsn_code, rate and gst_rate are snapshots of the item master at invoice time
    @Column(name = "hsn_code", nullable = false, length = 8)
    private String hsnCode;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "cgst", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgst;

    @Column(name = "sgst", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgst;

    @Column(name = "igst", nullable = false, precision = 14, scale = 2)
    private BigDecimal igst;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getTaxableValue() { return taxableValue; }
    public void setTaxableValue(BigDecimal taxableValue) { this.taxableValue = taxableValue; }
    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }
    public BigDecimal getCgst() { return cgst; }
    public void setCgst(BigDecimal cgst) { this.cgst = cgst; }
    public BigDecimal getSgst() { return sgst; }
    public void setSgst(BigDecimal sgst) { this.sgst = sgst; }
    public BigDecimal getIgst() { return igst; }
    public void setIgst(BigDecimal igst) { this.igst = igst; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
