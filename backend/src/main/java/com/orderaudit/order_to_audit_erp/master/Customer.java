package com.orderaudit.order_to_audit_erp.master;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 12)
    private CustomerType type;

    @Column(name = "gstin", length = 15)
    private String gstin;

    // GST state code, CHAR(2) in the database (33 = Tamil Nadu). Drives CGST/SGST vs IGST later.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state_code", nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String stateCode;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CustomerType getType() { return type; }
    public void setType(CustomerType type) { this.type = type; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
