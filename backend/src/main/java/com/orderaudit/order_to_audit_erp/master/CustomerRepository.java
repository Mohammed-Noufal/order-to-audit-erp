package com.orderaudit.order_to_audit_erp.master;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByActiveTrueOrderByIdAsc();
}
