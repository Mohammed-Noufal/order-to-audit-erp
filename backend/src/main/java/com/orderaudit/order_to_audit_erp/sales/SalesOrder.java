package com.orderaudit.order_to_audit_erp.sales;

import java.time.Instant;
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

// Named SalesOrder (not Order) because ORDER is a reserved word in HQL
@Entity
@Table(name = "orders")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 25)
    private String orderNo;

    // Generated on the salesman's phone. UNIQUE in the database, so a retry can never create a second order.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "client_uuid", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String clientUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "salesman_id", nullable = false)
    private Long salesmanId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 12)
    private OrderStatus status = OrderStatus.SUBMITTED;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "decided_by")
    private Long decidedBy;

    // Filled by the database default
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SalesOrderLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Long getSalesmanId() { return salesmanId; }
    public void setSalesmanId(Long salesmanId) { this.salesmanId = salesmanId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getDecidedBy() { return decidedBy; }
    public void setDecidedBy(Long decidedBy) { this.decidedBy = decidedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public List<SalesOrderLine> getLines() { return lines; }

    public void addLine(SalesOrderLine line) {
        lines.add(line);
        line.setSalesOrder(this);
    }
}
