package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.orderaudit.order_to_audit_erp.master.Customer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 25)
    private String invoiceNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "supply_type", nullable = false, length = 5)
    private SupplyType supplyType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "place_of_supply", nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String placeOfSupply;

    // Snapshot: later edits to the customer never change a posted invoice
    @Column(name = "customer_gstin", length = 15)
    private String customerGstin;

    @Column(name = "taxable_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "cgst", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO.setScale(2);

    @Column(name = "sgst", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO.setScale(2);

    @Column(name = "igst", nullable = false, precision = 14, scale = 2)
    private BigDecimal igst = BigDecimal.ZERO.setScale(2);

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private InvoiceStatus status = InvoiceStatus.POSTED;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public SupplyType getSupplyType() { return supplyType; }
    public void setSupplyType(SupplyType supplyType) { this.supplyType = supplyType; }
    public String getPlaceOfSupply() { return placeOfSupply; }
    public void setPlaceOfSupply(String placeOfSupply) { this.placeOfSupply = placeOfSupply; }
    public String getCustomerGstin() { return customerGstin; }
    public void setCustomerGstin(String customerGstin) { this.customerGstin = customerGstin; }
    public BigDecimal getTaxableValue() { return taxableValue; }
    public void setTaxableValue(BigDecimal taxableValue) { this.taxableValue = taxableValue; }
    public BigDecimal getCgst() { return cgst; }
    public void setCgst(BigDecimal cgst) { this.cgst = cgst; }
    public BigDecimal getSgst() { return sgst; }
    public void setSgst(BigDecimal sgst) { this.sgst = sgst; }
    public BigDecimal getIgst() { return igst; }
    public void setIgst(BigDecimal igst) { this.igst = igst; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public List<InvoiceLine> getLines() { return lines; }

    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }
}
