package com.orderaudit.order_to_audit_erp.sales;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salesman")
public class SalesmanController {

    private final OrderService orderService;

    public SalesmanController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/me/customer-sales")
    @PreAuthorize("hasRole('SALESMAN')")
    public List<SalesResponses.CustomerSales> customerSales() {
        return orderService.customerSales();
    }
}
