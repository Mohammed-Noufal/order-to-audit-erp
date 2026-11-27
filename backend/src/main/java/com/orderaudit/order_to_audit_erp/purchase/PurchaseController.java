package com.orderaudit.order_to_audit_erp.purchase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRODUCTION','ADMIN','OWNER','ACCOUNTANT')")
    public List<PurchaseResponse> list() {
        return purchaseService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCTION','ADMIN','OWNER','ACCOUNTANT')")
    public PurchaseResponse get(@PathVariable Long id) {
        return purchaseService.get(id);
    }

    // Only the production supervisor records raw-material receipts
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PRODUCTION')")
    public PurchaseResponse create(@Valid @RequestBody CreatePurchaseRequest request) {
        return purchaseService.create(request);
    }
}
