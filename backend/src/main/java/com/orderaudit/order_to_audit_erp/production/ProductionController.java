package com.orderaudit.order_to_audit_erp.production;

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
@RequestMapping("/api/production-runs")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRODUCTION','ADMIN','OWNER','ACCOUNTANT')")
    public List<ProductionRunResponse> list() {
        return productionService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCTION','ADMIN','OWNER','ACCOUNTANT')")
    public ProductionRunResponse get(@PathVariable Long id) {
        return productionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PRODUCTION')")
    public ProductionRunResponse create(@Valid @RequestBody ProductionRequests.CreateRun request) {
        return productionService.create(request);
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasRole('PRODUCTION')")
    public ProductionRunResponse issue(@PathVariable Long id,
                                       @Valid @RequestBody ProductionRequests.Issue request) {
        return productionService.issue(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('PRODUCTION')")
    public ProductionRunResponse complete(@PathVariable Long id,
                                          @Valid @RequestBody ProductionRequests.Complete request) {
        return productionService.complete(id, request);
    }

    @PostMapping("/{id}/overheads")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PRODUCTION')")
    public ProductionRunResponse addOverhead(@PathVariable Long id,
                                             @Valid @RequestBody ProductionRequests.Overhead request) {
        return productionService.addOverhead(id, request);
    }
}
