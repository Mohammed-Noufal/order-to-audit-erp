package com.orderaudit.order_to_audit_erp.sales;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','OWNER')")
    public List<InvoiceResponse> list() {
        return invoiceService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','OWNER')")
    public InvoiceResponse get(@PathVariable Long id) {
        return invoiceService.get(id);
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public SalesResponses.DispatchResult dispatch(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SalesRequests.DispatchInvoice request) {
        return invoiceService.dispatch(id, request != null ? request.dispatchDate() : null);
    }
}
