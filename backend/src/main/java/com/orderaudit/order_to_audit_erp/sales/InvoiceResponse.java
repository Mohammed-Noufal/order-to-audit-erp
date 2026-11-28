package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNo,
        Long orderId,
        Long customerId,
        String customerName,
        String customerGstin,
        LocalDate invoiceDate,
        SupplyType supplyType,
        String placeOfSupply,
        BigDecimal taxableValue,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal total,
        InvoiceStatus status,
        boolean dispatched,
        List<Line> lines) {

    public record Line(
            Long itemId,
            String itemCode,
            String itemName,
            String hsnCode,
            Integer qty,
            BigDecimal rate,
            BigDecimal taxableValue,
            BigDecimal gstRate,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal lineTotal) {
    }

    public static InvoiceResponse from(Invoice invoice, boolean dispatched) {
        List<Line> lines = invoice.getLines().stream()
                .map(l -> new Line(
                        l.getItem().getId(),
                        l.getItem().getCode(),
                        l.getItem().getName(),
                        l.getHsnCode(),
                        l.getQty(),
                        l.getRate(),
                        l.getTaxableValue(),
                        l.getGstRate(),
                        l.getCgst(),
                        l.getSgst(),
                        l.getIgst(),
                        l.getLineTotal()))
                .toList();
        return new InvoiceResponse(
                invoice.getId(), invoice.getInvoiceNo(), invoice.getOrderId(),
                invoice.getCustomer().getId(), invoice.getCustomer().getName(), invoice.getCustomerGstin(),
                invoice.getInvoiceDate(), invoice.getSupplyType(), invoice.getPlaceOfSupply(),
                invoice.getTaxableValue(), invoice.getCgst(), invoice.getSgst(), invoice.getIgst(),
                invoice.getTotal(), invoice.getStatus(), dispatched, lines);
    }
}
