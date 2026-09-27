package com.villageone.app.service;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.*;
import com.villageone.app.repository.*;
import com.villageone.app.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository                 invoiceRepo;
    private final InvoiceItemRepository             itemRepo;
    private final ProductRepository                 productRepo;
    private final ProductService                    productService;
    private final BusinessInvoiceSequenceRepository sequenceRepo;
    private final UserRepository                    userRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Transactional
    public InvoiceResponse createInvoice(String ownerUsername, String createdBy, InvoiceRequest req) {
        sequenceRepo.ensureExists(ownerUsername);
        BusinessInvoiceSequence seq = sequenceRepo
                .findByOwnerUsernameForUpdate(ownerUsername)
                .orElseThrow(() -> new RuntimeException("Failed to init invoice sequence for: " + ownerUsername));
        seq.setLastSequence(seq.getLastSequence() + 1);
        sequenceRepo.saveAndFlush(seq);

        String invoiceNumber = "INV-" + String.format("%04d", seq.getLastSequence());
        Long businessId = JwtAuthFilter.BUSINESS_ID.get();
        log.info("Creating invoice: owner={} invoiceNumber={} createdBy={}", ownerUsername, invoiceNumber, createdBy);

        Invoice invoice = Invoice.builder()
                .businessId(businessId).invoiceNumber(invoiceNumber)
                .ownerUsername(ownerUsername).createdByUsername(createdBy)
                .customerName(req.getCustomerName()).customerMobile(req.getCustomerMobile())
                .customerAddress(req.getCustomerAddress())
                .paymentMode(Invoice.PaymentMode.valueOf(req.getPaymentMode()))
                .status(Invoice.InvoiceStatus.PAID)
                .subTotal(BigDecimal.ZERO).totalGst(BigDecimal.ZERO).grandTotal(BigDecimal.ZERO)
                .build();

        Invoice saved = invoiceRepo.save(invoice);
        BigDecimal sub = BigDecimal.ZERO, gst = BigDecimal.ZERO;
        List<InvoiceItemResponse> responses = new ArrayList<>();

        for (InvoiceItemRequest ir : req.getItems()) {
            Product p = productRepo.findById(ir.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + ir.getProductId()));
            BigDecimal lineTotal  = p.getSellingPrice().multiply(ir.getQuantity());
            BigDecimal gstAmt     = lineTotal.multiply(p.getGstPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(saved).productId(p.getId()).productName(p.getName()).unit(p.getUnit())
                    .quantity(ir.getQuantity()).unitPrice(p.getSellingPrice())
                    .purchasePricePerUnit(p.getPurchasePrice()).gstPercent(p.getGstPercent())
                    .gstAmount(gstAmt).totalPrice(lineTotal.add(gstAmt)).warranty(ir.getWarranty())
                    .build();
            itemRepo.save(item);
            sub = sub.add(lineTotal); gst = gst.add(gstAmt);
            productService.deductStock(p.getId(), ir.getQuantity());
            responses.add(toItemResp(item));
        }

        saved.setSubTotal(sub); saved.setTotalGst(gst); saved.setGrandTotal(sub.add(gst));
        invoiceRepo.save(saved);
        log.info("Invoice created: owner={} invoiceNumber={} grandTotal={}", ownerUsername, invoiceNumber, saved.getGrandTotal());
        return buildResponse(saved, responses);
    }

    public List<InvoiceResponse> getInvoices(String ownerUsername) {
        return invoiceRepo.findByOwnerUsernameOrderByCreatedAtDesc(ownerUsername).stream().map(this::toResponse).toList();
    }

    public InvoiceResponse getInvoice(String invoiceNumber, String ownerUsername) {
        Invoice inv = invoiceRepo.findByInvoiceNumberAndOwnerUsername(invoiceNumber, ownerUsername)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber + " for owner: " + ownerUsername));
        return toResponse(inv);
    }

    @Transactional
    public InvoiceResponse updateInvoice(String invoiceNumber, String ownerUsername, InvoiceUpdateRequest req) {
        Invoice inv = invoiceRepo.findByInvoiceNumberAndOwnerUsername(invoiceNumber, ownerUsername)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
        if (req.getPaymentMode() != null && !req.getPaymentMode().isBlank())
            inv.setPaymentMode(Invoice.PaymentMode.valueOf(req.getPaymentMode()));
        if (req.getStatus() != null && !req.getStatus().isBlank())
            inv.setStatus(Invoice.InvoiceStatus.valueOf(req.getStatus()));
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            itemRepo.deleteByInvoiceId(inv.getId());
            BigDecimal sub = BigDecimal.ZERO, gst = BigDecimal.ZERO;
            for (InvoiceItemUpdateRequest ir : req.getItems()) {
                Product p = productRepo.findById(ir.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + ir.getProductId()));
                BigDecimal lineTotal = p.getSellingPrice().multiply(ir.getQuantity());
                BigDecimal gstAmt    = lineTotal.multiply(p.getGstPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                InvoiceItem item = InvoiceItem.builder()
                        .invoice(inv).productId(p.getId()).productName(p.getName()).unit(p.getUnit())
                        .quantity(ir.getQuantity()).unitPrice(p.getSellingPrice())
                        .purchasePricePerUnit(p.getPurchasePrice()).gstPercent(p.getGstPercent())
                        .gstAmount(gstAmt).totalPrice(lineTotal.add(gstAmt)).warranty(ir.getWarranty()).build();
                itemRepo.save(item);
                sub = sub.add(lineTotal); gst = gst.add(gstAmt);
            }
            inv.setSubTotal(sub); inv.setTotalGst(gst); inv.setGrandTotal(sub.add(gst));
        }
        return toResponse(invoiceRepo.save(inv));
    }

    @Transactional
    public void deleteInvoice(String invoiceNumber, String ownerUsername) {
        Invoice inv = invoiceRepo.findByInvoiceNumberAndOwnerUsername(invoiceNumber, ownerUsername)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
        itemRepo.deleteByInvoiceId(inv.getId());
        invoiceRepo.delete(inv);
        log.info("Invoice deleted: owner={} invoiceNumber={}", ownerUsername, invoiceNumber);
    }

    private InvoiceResponse toResponse(Invoice inv) {
        return buildResponse(inv, itemRepo.findByInvoiceId(inv.getId()).stream().map(this::toItemResp).toList());
    }

    private InvoiceResponse buildResponse(Invoice inv, List<InvoiceItemResponse> items) {
        String createdByName = userRepo.findByUsername(inv.getCreatedByUsername()).map(User::getFullName).orElse(inv.getCreatedByUsername());
        return InvoiceResponse.builder()
                .id(inv.getId()).invoiceNumber(inv.getInvoiceNumber())
                .customerName(inv.getCustomerName()).customerMobile(inv.getCustomerMobile())
                .customerAddress(inv.getCustomerAddress()).subTotal(inv.getSubTotal())
                .totalGst(inv.getTotalGst()).grandTotal(inv.getGrandTotal())
                .paymentMode(inv.getPaymentMode().name()).status(inv.getStatus().name())
                .createdByUsername(inv.getCreatedByUsername()).createdByName(createdByName)
                .createdAt(inv.getCreatedAt().format(FMT)).items(items).build();
    }

    private InvoiceItemResponse toItemResp(InvoiceItem i) {
        return InvoiceItemResponse.builder()
                .id(i.getId()).productId(i.getProductId()).productName(i.getProductName())
                .unit(i.getUnit()).quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                .gstPercent(i.getGstPercent()).gstAmount(i.getGstAmount()).totalPrice(i.getTotalPrice())
                .warranty(i.getWarranty() != null ? i.getWarranty() : "").build();
    }
}