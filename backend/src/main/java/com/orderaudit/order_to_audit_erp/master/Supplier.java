package com.orderaudit.order_to_audit_erp.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "phone", length = 15)
    private String phone;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
