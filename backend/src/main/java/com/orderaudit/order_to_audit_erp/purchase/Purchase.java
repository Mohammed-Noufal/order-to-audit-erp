package com.orderaudit.order_to_audit_erp.purchase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.orderaudit.order_to_audit_erp.master.Supplier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_no", nullable = false, length = 25)
    private String purchaseNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "supplier_bill_no", length = 40)
    private String supplierBillNo;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PurchaseLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String purchaseNo) { this.purchaseNo = purchaseNo; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getSupplierBillNo() { return supplierBillNo; }
    public void setSupplierBillNo(String supplierBillNo) { this.supplierBillNo = supplierBillNo; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public List<PurchaseLine> getLines() { return lines; }

    public void addLine(PurchaseLine line) {
        lines.add(line);
        line.setPurchase(this);
    }
}
