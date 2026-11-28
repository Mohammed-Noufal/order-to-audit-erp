package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.audit.AuditService;
import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;
import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.common.ConflictException;
import com.orderaudit.order_to_audit_erp.common.DocSequenceService;
import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;
import com.orderaudit.order_to_audit_erp.master.Customer;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.ItemRepository;
import com.orderaudit.order_to_audit_erp.master.Setting;
import com.orderaudit.order_to_audit_erp.master.SettingRepository;
import com.orderaudit.order_to_audit_erp.stock.MovementType;
import com.orderaudit.order_to_audit_erp.stock.SourceType;
import com.orderaudit.order_to_audit_erp.stock.StockService;

@Service
public class InvoiceService {

    private static final String DEFAULT_FACTORY_STATE = "33"; // Tamil Nadu

    private final InvoiceRepository invoiceRepository;
    private final DispatchRepository dispatchRepository;
    private final SalesOrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final SettingRepository settingRepository;
    private final StockService stockService;
    private final DocSequenceService docSequenceService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          DispatchRepository dispatchRepository,
                          SalesOrderRepository orderRepository,
                          ItemRepository itemRepository,
                          SettingRepository settingRepository,
                          StockService stockService,
                          DocSequenceService docSequenceService,
                          CurrentUserService currentUserService,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.dispatchRepository = dispatchRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.settingRepository = settingRepository;
        this.stockService = stockService;
        this.docSequenceService = docSequenceService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ create GST invoice

    @Transactional
    public InvoiceResponse create(Long orderId, LocalDate invoiceDate) {
        SalesOrder order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new ConflictException("An order must be APPROVED before it is invoiced (order "
                    + order.getOrderNo() + " is " + order.getStatus() + ")");
        }
        if (invoiceRepository.existsByOrderId(orderId)) {
            throw new ConflictException("An invoice already exists for order " + order.getOrderNo());
        }

        LocalDate date = (invoiceDate != null) ? invoiceDate : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Invoice date cannot be in the future");
        }

        // Same state as the factory: CGST + SGST. Any other state: IGST.
        Customer customer = order.getCustomer();
        boolean intraState = factoryStateCode().equalsIgnoreCase(customer.getStateCode().trim());

        Invoice invoice = new Invoice();
        invoice.setOrderId(order.getId());
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(date);
        invoice.setSupplyType(intraState ? SupplyType.INTRA : SupplyType.INTER);
        invoice.setPlaceOfSupply(customer.getStateCode());
        invoice.setCustomerGstin(customer.getGstin());
        invoice.setStatus(InvoiceStatus.POSTED);

        BigDecimal taxable = BigDecimal.ZERO.setScale(2);
        BigDecimal cgst = BigDecimal.ZERO.setScale(2);
        BigDecimal sgst = BigDecimal.ZERO.setScale(2);
        BigDecimal igst = BigDecimal.ZERO.setScale(2);

        for (SalesOrderLine orderLine : order.getLines()) {
            int qty = orderLine.effectiveQty();
            if (qty <= 0) {
                continue; // the admin zeroed this line
            }
            Item item = orderLine.getItem();
            if (item.getHsnCode() == null || item.getGstRate() == null) {
                throw new BadRequestException("Item " + item.getCode() + " has no HSN code or GST rate configured");
            }

            GstCalculator.LineTax tax = GstCalculator.calculate(
                    qty, orderLine.getUnitPrice(), item.getGstRate(), intraState);

            InvoiceLine line = new InvoiceLine();
            line.setItem(item);
            line.setHsnCode(item.getHsnCode());
            line.setQty(qty);
            line.setRate(orderLine.getUnitPrice());
            line.setTaxableValue(tax.taxableValue());
            line.setGstRate(item.getGstRate());
            line.setCgst(tax.cgst());
            line.setSgst(tax.sgst());
            line.setIgst(tax.igst());
            line.setLineTotal(tax.lineTotal());
            invoice.addLine(line);

            taxable = taxable.add(tax.taxableValue());
            cgst = cgst.add(tax.cgst());
            sgst = sgst.add(tax.sgst());
            igst = igst.add(tax.igst());
        }
        if (invoice.getLines().isEmpty()) {
            throw new BadRequestException("The order has no billable quantity");
        }

        invoice.setTaxableValue(taxable);
        invoice.setCgst(cgst);
        invoice.setSgst(sgst);
        invoice.setIgst(igst);
        invoice.setTotal(taxable.add(cgst).add(sgst).add(igst)); // no round-off (assumption for the README)

        // Number is taken last, so a failure above never burns an invoice number
        invoice.setInvoiceNo(docSequenceService.next("INV", date));

        Invoice saved = invoiceRepository.save(invoice);
        order.setStatus(OrderStatus.INVOICED);

        auditService.log("INVOICE", saved.getId(), "CREATE", null,
                Map.of("invoiceNo", saved.getInvoiceNo(),
                        "orderId", order.getId(),
                        "supplyType", saved.getSupplyType(),
                        "total", saved.getTotal()));
        return InvoiceResponse.from(saved, false);
    }

    // ------------------------------------------------------------------ dispatch (stock leaves)

    @Transactional
    public SalesResponses.DispatchResult dispatch(Long invoiceId, LocalDate dispatchDate) {
        Invoice invoice = invoiceRepository.lockById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        if (invoice.getStatus() != InvoiceStatus.POSTED) {
            throw new ConflictException("Only POSTED invoices can be dispatched");
        }
        if (dispatchRepository.existsByInvoiceId(invoiceId)) {
            throw new ConflictException("Invoice " + invoice.getInvoiceNo() + " has already been dispatched");
        }

        LocalDate date = (dispatchDate != null) ? dispatchDate : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Dispatch date cannot be in the future");
        }
        if (date.isBefore(invoice.getInvoiceDate())) {
            throw new BadRequestException("Dispatch date cannot be before the invoice date");
        }

        // Hard stock check: StockService throws a 409 if any line would take the balance below zero,
        // and the whole transaction (including the lines already posted) rolls back.
        // Lines go in item-id order so two dispatches can never deadlock each other.
        List<InvoiceLine> ordered = invoice.getLines().stream()
                .sorted(Comparator.comparing((InvoiceLine l) -> l.getItem().getId()))
                .toList();
        for (InvoiceLine line : ordered) {
            Item item = itemRepository.lockById(line.getItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + line.getItem().getId()));
            stockService.post(item, MovementType.SALE_DISPATCH,
                    BigDecimal.valueOf(line.getQty()).negate(), item.getAvgCost(),
                    SourceType.INVOICE, invoice.getId(), date);
        }

        Dispatch dispatch = new Dispatch();
        dispatch.setInvoiceId(invoice.getId());
        dispatch.setDispatchDate(date);
        dispatch.setDispatchedBy(currentUserService.requireCurrentUser().getId());
        Dispatch saved = dispatchRepository.save(dispatch);

        SalesOrder order = orderRepository.lockById(invoice.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + invoice.getOrderId()));
        order.setStatus(OrderStatus.DISPATCHED);

        auditService.log("INVOICE", invoice.getId(), "DISPATCH",
                Map.of("orderStatus", OrderStatus.INVOICED),
                Map.of("orderStatus", OrderStatus.DISPATCHED, "dispatchDate", date.toString()));
        return new SalesResponses.DispatchResult(
                saved.getId(), invoice.getId(), invoice.getInvoiceNo(), date, order.getStatus());
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list() {
        List<Invoice> invoices = invoiceRepository.findAllByOrderByInvoiceDateDescIdDesc();
        if (invoices.isEmpty()) {
            return List.of();
        }
        Set<Long> dispatchedIds = dispatchRepository
                .findByInvoiceIdIn(invoices.stream().map(Invoice::getId).toList()).stream()
                .map(Dispatch::getInvoiceId)
                .collect(Collectors.toSet());
        return invoices.stream()
                .map(i -> InvoiceResponse.from(i, dispatchedIds.contains(i.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id) {
        Invoice invoice = invoiceRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return InvoiceResponse.from(invoice, dispatchRepository.existsByInvoiceId(id));
    }

    // ------------------------------------------------------------------ helpers

    private String factoryStateCode() {
        return settingRepository.findById("factory_state_code")
                .map(Setting::getSettingValue)
                .orElse(DEFAULT_FACTORY_STATE);
    }
}
