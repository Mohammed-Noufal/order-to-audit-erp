package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.audit.AuditService;
import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;
import com.orderaudit.order_to_audit_erp.auth.Role;
import com.orderaudit.order_to_audit_erp.auth.User;
import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.common.ConflictException;
import com.orderaudit.order_to_audit_erp.common.DocSequenceService;
import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;
import com.orderaudit.order_to_audit_erp.master.Customer;
import com.orderaudit.order_to_audit_erp.master.CustomerRepository;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.ItemRepository;
import com.orderaudit.order_to_audit_erp.master.ItemType;
import com.orderaudit.order_to_audit_erp.stock.StockService;

import jakarta.persistence.EntityManager;

@Service
public class OrderService {

    private final SalesOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final StockService stockService;
    private final DocSequenceService docSequenceService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final EntityManager em;

    public OrderService(SalesOrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ItemRepository itemRepository,
                        StockService stockService,
                        DocSequenceService docSequenceService,
                        CurrentUserService currentUserService,
                        AuditService auditService,
                        EntityManager em) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.itemRepository = itemRepository;
        this.stockService = stockService;
        this.docSequenceService = docSequenceService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.em = em;
    }

    /** created = true for a new order, false when the same clientUuid was already received (a retry). */
    public record SubmitResult(OrderResponse order, boolean created) {
    }

    // ------------------------------------------------------------------ salesman side

    @Transactional
    public SubmitResult submit(SalesRequests.SubmitOrder request) {
        User salesman = currentUserService.requireCurrentUser();
        String clientUuid = request.clientUuid().toLowerCase(Locale.ROOT);

        // Retry or offline-sync replay: return the order we already have instead of making a second one
        Optional<SalesOrder> existing = orderRepository.findByClientUuid(clientUuid);
        if (existing.isPresent()) {
            if (!existing.get().getSalesmanId().equals(salesman.getId())) {
                throw new ConflictException("This clientUuid is already used by another order");
            }
            return new SubmitResult(OrderResponse.from(existing.get()), false);
        }

        Customer customer = customerRepository.findById(request.customerId())
                .filter(Customer::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        SalesOrder order = new SalesOrder();
        order.setOrderNo(docSequenceService.next("ORD", LocalDate.now()));
        order.setClientUuid(clientUuid);
        order.setCustomer(customer);
        order.setSalesmanId(salesman.getId());
        order.setStatus(OrderStatus.SUBMITTED);
        order.setNote(request.note());

        for (SalesRequests.OrderLine lineRequest : request.lines()) {
            Item item = itemRepository.findById(lineRequest.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + lineRequest.itemId()));
            // An item must be fully configured for sale, or the invoice could carry the wrong tax
            if (!item.isActive() || item.getItemType() != ItemType.FINISHED
                    || item.getSellingPrice() == null || item.getGstRate() == null || item.getHsnCode() == null) {
                throw new BadRequestException("Item " + item.getCode() + " is not available for sale");
            }
            SalesOrderLine line = new SalesOrderLine();
            line.setItem(item);
            line.setOrderedQty(lineRequest.orderedQty());
            line.setUnitPrice(item.getSellingPrice().setScale(2, RoundingMode.HALF_UP)); // price snapshot
            order.addLine(line);
        }

        SalesOrder saved = orderRepository.saveAndFlush(order);
        em.refresh(saved); // read back created_at, which the database fills in
        return new SubmitResult(OrderResponse.from(saved), true);
    }

    @Transactional(readOnly = true)
    public List<SalesResponses.CustomerSales> customerSales() {
        User salesman = currentUserService.requireCurrentUser();
        List<SalesOrder> orders = orderRepository.findBySalesmanIdOrderByCreatedAtDescIdDesc(salesman.getId())
                .stream()
                .filter(o -> o.getStatus() != OrderStatus.REJECTED && o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        Map<Long, List<SalesOrder>> byCustomer = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCustomer().getId(), LinkedHashMap::new, Collectors.toList()));

        return byCustomer.values().stream()
                .map(group -> {
                    Customer customer = group.get(0).getCustomer();
                    BigDecimal total = group.stream()
                            .flatMap(o -> o.getLines().stream())
                            .map(SalesOrderLine::lineValue)
                            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
                    return new SalesResponses.CustomerSales(
                            customer.getId(), customer.getName(), group.size(), total);
                })
                .toList();
    }

    // ------------------------------------------------------------------ reads (role-scoped)

    @Transactional(readOnly = true)
    public List<OrderResponse> list(OrderStatus status) {
        User current = currentUserService.requireCurrentUser();
        List<SalesOrder> orders;
        if (current.getRole() == Role.SALESMAN) {
            // A salesman only ever sees his own orders
            orders = (status != null)
                    ? orderRepository.findBySalesmanIdAndStatusOrderByCreatedAtDescIdDesc(current.getId(), status)
                    : orderRepository.findBySalesmanIdOrderByCreatedAtDescIdDesc(current.getId());
        } else {
            orders = (status != null)
                    ? orderRepository.findByStatusOrderByCreatedAtDescIdDesc(status)
                    : orderRepository.findAllByOrderByCreatedAtDescIdDesc();
        }
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        SalesOrder order = orderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        User current = currentUserService.requireCurrentUser();
        if (current.getRole() == Role.SALESMAN && !order.getSalesmanId().equals(current.getId())) {
            // 404, not 403: do not confirm that someone else's order exists
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        return OrderResponse.from(order);
    }

    // ------------------------------------------------------------------ admin side

    @Transactional(readOnly = true)
    public List<SalesResponses.StockCheckLine> stockCheck(Long orderId) {
        SalesOrder order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return checkLines(order);
    }

    @Transactional
    public OrderResponse edit(Long orderId, SalesRequests.EditOrder request) {
        SalesOrder order = requireLockedOrder(orderId);
        requireSubmitted(order, "edited");
        Map<String, Object> before = snapshot(order);

        for (SalesRequests.EditLine edit : request.lines()) {
            if (edit.approvedQty() == null && edit.unitPrice() == null) {
                throw new BadRequestException("Line " + edit.lineId() + ": give an approvedQty, a unitPrice, or both");
            }
            SalesOrderLine line = order.getLines().stream()
                    .filter(l -> l.getId().equals(edit.lineId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "Line " + edit.lineId() + " does not belong to order " + order.getOrderNo()));
            if (edit.approvedQty() != null) {
                line.setApprovedQty(edit.approvedQty());
            }
            if (edit.unitPrice() != null) {
                line.setUnitPrice(edit.unitPrice().setScale(2, RoundingMode.HALF_UP));
            }
        }

        SalesOrder saved = orderRepository.save(order);
        auditService.log("ORDER", saved.getId(), "EDIT", before, snapshot(saved));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse approve(Long orderId, boolean allowBackorder) {
        SalesOrder order = requireLockedOrder(orderId);
        requireSubmitted(order, "approved");

        // Advisory check: the admin sees the shortage and decides
        List<SalesResponses.StockCheckLine> shortages = checkLines(order).stream()
                .filter(s -> s.shortageQty().signum() > 0)
                .toList();
        if (!shortages.isEmpty() && !allowBackorder) {
            String detail = shortages.stream()
                    .map(s -> "%s (%s): requested %d, available %s, shortage %s".formatted(
                            s.itemName(), s.itemCode(), s.requestedQty(),
                            s.availableQty().toPlainString(), s.shortageQty().toPlainString()))
                    .collect(Collectors.joining("; "));
            throw new ConflictException("Insufficient finished-goods stock for approval: " + detail
                    + ". Reduce the quantity, reject the order, or approve with allowBackorder=true");
        }

        for (SalesOrderLine line : order.getLines()) {
            if (line.getApprovedQty() == null) {
                line.setApprovedQty(line.getOrderedQty());
            }
        }
        boolean nothingBillable = order.getLines().stream().allMatch(l -> l.effectiveQty() <= 0);
        if (nothingBillable) {
            throw new BadRequestException("Every line has quantity 0, so there is nothing to approve: reject the order instead");
        }

        order.setStatus(OrderStatus.APPROVED);
        order.setDecidedBy(currentUserService.requireCurrentUser().getId());
        SalesOrder saved = orderRepository.save(order);
        auditService.log("ORDER", saved.getId(), "APPROVE",
                Map.of("status", OrderStatus.SUBMITTED),
                Map.of("status", OrderStatus.APPROVED, "allowBackorder", allowBackorder));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse reject(Long orderId, String reason) {
        SalesOrder order = requireLockedOrder(orderId);
        requireSubmitted(order, "rejected");

        String cleanReason = (reason == null) ? "" : reason.trim();
        // orders.note is only 255 characters: keep the salesman's note and append a short reason.
        // The full reason is stored in the audit log.
        String suffix = cleanReason.isEmpty() ? "Rejected" : "Rejected: " + cleanReason;
        String base = order.getNote();
        String combined = (base == null || base.isBlank()) ? suffix : base + " | " + suffix;
        order.setNote(combined.length() > 255 ? combined.substring(0, 255) : combined);

        order.setStatus(OrderStatus.REJECTED);
        order.setDecidedBy(currentUserService.requireCurrentUser().getId());
        SalesOrder saved = orderRepository.save(order);
        auditService.log("ORDER", saved.getId(), "REJECT",
                Map.of("status", OrderStatus.SUBMITTED),
                Map.of("status", OrderStatus.REJECTED, "reason", cleanReason));
        return OrderResponse.from(saved);
    }

    // ------------------------------------------------------------------ helpers

    private SalesOrder requireLockedOrder(Long orderId) {
        return orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void requireSubmitted(SalesOrder order, String verb) {
        if (order.getStatus() != OrderStatus.SUBMITTED) {
            throw new ConflictException("Only SUBMITTED orders can be " + verb
                    + " (order " + order.getOrderNo() + " is " + order.getStatus() + ")");
        }
    }

    /** Requested quantity per product (lines of the same product are added up) against the stock balance. */
    private List<SalesResponses.StockCheckLine> checkLines(SalesOrder order) {
        Map<Long, Integer> requested = new LinkedHashMap<>();
        Map<Long, Item> items = new LinkedHashMap<>();
        for (SalesOrderLine line : order.getLines()) {
            requested.merge(line.getItem().getId(), line.effectiveQty(), Integer::sum);
            items.putIfAbsent(line.getItem().getId(), line.getItem());
        }

        List<SalesResponses.StockCheckLine> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : requested.entrySet()) {
            Item item = items.get(entry.getKey());
            BigDecimal available = stockService.balance(item.getId());
            BigDecimal shortage = BigDecimal.valueOf(entry.getValue()).subtract(available)
                    .max(BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);
            result.add(new SalesResponses.StockCheckLine(
                    item.getId(), item.getCode(), item.getName(), entry.getValue(), available, shortage));
        }
        return result;
    }

    /** Flat before/after view of an order's lines for the audit log. */
    private Map<String, Object> snapshot(SalesOrder order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (SalesOrderLine line : order.getLines()) {
            snapshot.put("line" + line.getId() + "Qty", line.effectiveQty());
            snapshot.put("line" + line.getId() + "Price", line.getUnitPrice());
        }
        return snapshot;
    }
}
