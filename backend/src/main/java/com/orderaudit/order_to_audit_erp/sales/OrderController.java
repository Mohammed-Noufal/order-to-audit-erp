package com.orderaudit.order_to_audit_erp.sales;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    public OrderController(OrderService orderService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    // 201 for a new order, 200 when the same clientUuid is sent again (a retry)
    @PostMapping
    @PreAuthorize("hasRole('SALESMAN')")
    public ResponseEntity<OrderResponse> submit(@Valid @RequestBody SalesRequests.SubmitOrder request) {
        OrderService.SubmitResult result = orderService.submit(request);
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.order());
    }

    // Salesmen see only their own orders (enforced in the service)
    @GetMapping
    @PreAuthorize("hasAnyRole('SALESMAN','ADMIN','OWNER','ACCOUNTANT')")
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status) {
        return orderService.list(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALESMAN','ADMIN','OWNER','ACCOUNTANT')")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @GetMapping("/{id}/stock-check")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SalesResponses.StockCheckLine> stockCheck(@PathVariable Long id) {
        return orderService.stockCheck(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse edit(@PathVariable Long id, @Valid @RequestBody SalesRequests.EditOrder request) {
        return orderService.edit(id, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse approve(@PathVariable Long id,
                                 @Valid @RequestBody(required = false) SalesRequests.Approve request) {
        boolean allowBackorder = request != null && Boolean.TRUE.equals(request.allowBackorder());
        return orderService.approve(id, allowBackorder);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse reject(@PathVariable Long id,
                                @Valid @RequestBody(required = false) SalesRequests.Reject request) {
        return orderService.reject(id, request != null ? request.reason() : null);
    }

    @PostMapping("/{id}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceResponse createInvoice(@PathVariable Long id,
                                         @Valid @RequestBody(required = false) SalesRequests.CreateInvoice request) {
        return invoiceService.create(id, request != null ? request.invoiceDate() : null);
    }
}
