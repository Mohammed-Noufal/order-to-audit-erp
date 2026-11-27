package com.orderaudit.order_to_audit_erp.stock;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class TempStockController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION','ACCOUNTANT','OWNER')")
    public Map<String, String> stockPlaceholder() {
        return Map.of("message", "Temporary endpoint: RBAC check only, no stock data yet");
    }
}