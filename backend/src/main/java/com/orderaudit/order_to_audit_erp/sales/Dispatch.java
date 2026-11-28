package com.orderaudit.order_to_audit_erp.sales;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dispatches")
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "dispatched_by", nullable = false)
    private Long dispatchedBy;

    public Long getId() { return id; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public LocalDate getDispatchDate() { return dispatchDate; }
    public void setDispatchDate(LocalDate dispatchDate) { this.dispatchDate = dispatchDate; }
    public Long getDispatchedBy() { return dispatchedBy; }
    public void setDispatchedBy(Long dispatchedBy) { this.dispatchedBy = dispatchedBy; }
}
