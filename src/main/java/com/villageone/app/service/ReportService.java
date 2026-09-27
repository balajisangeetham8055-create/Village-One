package com.villageone.app.service;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.Invoice;
import com.villageone.app.repository.InvoiceItemRepository;
import com.villageone.app.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final InvoiceRepository     invoiceRepo;
    private final InvoiceItemRepository itemRepo;

    public SalesReportResponse getSalesReport(String ownerUsername, ReportRequest req) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime from = LocalDate.parse(req.getFromDate(), fmt).atStartOfDay();
        LocalDateTime to   = LocalDate.parse(req.getToDate(),   fmt).atTime(23, 59, 59);
        var invoices = invoiceRepo.findByOwnerAndDateRange(ownerUsername, from, to);
        BigDecimal revenue = BigDecimal.ZERO, gst = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO, upi = BigDecimal.ZERO, card = BigDecimal.ZERO, credit = BigDecimal.ZERO, cost = BigDecimal.ZERO;
        for (var inv : invoices) {
            revenue = revenue.add(inv.getGrandTotal()); gst = gst.add(inv.getTotalGst());
            switch (inv.getPaymentMode()) {
                case CASH   -> cash   = cash.add(inv.getGrandTotal());
                case UPI    -> upi    = upi.add(inv.getGrandTotal());
                case CARD   -> card   = card.add(inv.getGrandTotal());
                case CREDIT -> credit = credit.add(inv.getGrandTotal());
            }
            for (var item : itemRepo.findByInvoiceId(inv.getId())) {
                BigDecimal pp = item.getPurchasePricePerUnit() != null ? item.getPurchasePricePerUnit() : BigDecimal.ZERO;
                cost = cost.add(pp.multiply(item.getQuantity()));
            }
        }
        return SalesReportResponse.builder()
                .fromDate(req.getFromDate()).toDate(req.getToDate())
                .totalInvoices(invoices.size()).totalRevenue(revenue).totalGstCollected(gst)
                .totalProfit(revenue.subtract(gst).subtract(cost))
                .cashSales(cash).upiSales(upi).cardSales(card).creditSales(credit).build();
    }
}