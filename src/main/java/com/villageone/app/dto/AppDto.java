package com.villageone.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

public class AppDto {

    // ══════════════════════════════════════════════════════
    //  AUTH
    // ══════════════════════════════════════════════════════
    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @Pattern(regexp = "\\d{10}|") private String mobile;
        @NotBlank private String password;
    }

    @Data @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String  token;
        private String  role;
        private String  username;
        private String  storeName;
        private String  planId;
        private String  planLabel;
        private String  planStart;
        private String  planExpiry;
        private int     planDaysLeft;
        private boolean planExpired;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @NotBlank @Pattern(regexp = "\\d{10}") private String mobile;
        @NotBlank @Email private String email;
        @NotBlank private String address;
        @NotBlank @Pattern(regexp = "\\d{6}") private String pincode;
        @NotBlank private String storeName;
        @NotBlank private String storeType;
        @NotBlank private String storeAddress;
        @NotBlank private String city;
        @NotBlank private String state;
        @NotBlank @Pattern(regexp = "\\d{6}") private String storePincode;
        private String gstNumber;
        @NotBlank private String planId;
        @NotBlank private String transactionId;
        @NotBlank @Size(min = 4) private String username;
        @NotBlank @Size(min = 6) private String password;
        private String role;
    }

    @Data @AllArgsConstructor
    public static class RegisterResponse {
        private String message;
        private String username;
        private String storeName;
        private String planId;
        private Integer daysActivated;
        private String planExpiry;
    }

    // ══════════════════════════════════════════════════════
    //  SUPPORT
    // ══════════════════════════════════════════════════════
    @Data
    public static class TicketRequest {
        @NotBlank private String issueType;
        @NotBlank private String priority;
        @NotBlank private String name;
        @NotBlank @Pattern(regexp = "\\d{10}") private String mobile;
        private String email;
        @NotBlank private String subject;
        @NotBlank private String description;
    }

    @Data @AllArgsConstructor
    public static class TicketResponse {
        private String ticketId;
        private String message;
        private String whatsappFollowUp;
    }

    // ══════════════════════════════════════════════════════
    //  PRODUCT
    // ══════════════════════════════════════════════════════
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductRequest {
        @NotBlank private String name;
        private String shortCode;
        private String category;
        @NotBlank private String unit;
        @NotNull private BigDecimal purchasePrice;
        @NotNull private BigDecimal sellingPrice;
        @NotNull private BigDecimal gstPercent;
        @NotNull private Double  stockQuantity;
        @NotNull private Double  lowStockThreshold;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductResponse {
        private Long id;
        private String name;
        private String shortCode;
        private String category;
        private String unit;
        private BigDecimal purchasePrice;
        private BigDecimal sellingPrice;
        private BigDecimal gstPercent;
        private Double  stockQuantity;
        private Double  lowStockThreshold;
        private boolean lowStock;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockUpdateRequest {
        @NotNull private Long productId;
        @NotNull private Double  addQuantity;
    }

    // ══════════════════════════════════════════════════════
    //  INVOICE
    // ══════════════════════════════════════════════════════
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvoiceItemRequest {
        @NotNull private Long productId;
        @NotNull private BigDecimal quantity;
        private String warranty;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvoiceRequest {
        @NotBlank private String customerName;
        private String customerMobile;
        private String customerAddress;
        @NotNull private List<InvoiceItemRequest> items;
        @NotBlank private String paymentMode;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvoiceItemUpdateRequest {
        private Long productId;
        private BigDecimal quantity;
        private String warranty;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvoiceUpdateRequest {
        private String paymentMode;
        private String status;
        private List<InvoiceItemUpdateRequest> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvoiceItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal gstPercent;
        private BigDecimal gstAmount;
        private BigDecimal totalPrice;
        private String warranty;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private String customerName;
        private String customerMobile;
        private String customerAddress;
        private BigDecimal subTotal;
        private BigDecimal totalGst;
        private BigDecimal grandTotal;
        private String paymentMode;
        private String status;
        private String createdByUsername;
        private String createdByName;   // full name of who created invoice
        private String createdAt;
        private List<InvoiceItemResponse> items;
    }

    // ══════════════════════════════════════════════════════
    //  WORKER
    // ══════════════════════════════════════════════════════
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WorkerRequest {
        @NotBlank private String workerUsername;
        @NotBlank private String workerName;
        @NotBlank private String workerMobile;
        @NotBlank private String workerPassword;
        private boolean canCreateInvoice;
        private boolean canEditInvoice;
        private boolean canDeleteInvoice;
        private boolean canViewReports;
        private boolean canManageStock;
        private boolean canViewProducts;
        private boolean canAddProduct;
        private boolean canEditProduct;
        private boolean canDeleteProduct;
        private String  planId;           // monthly / half_yearly / yearly
        private String  transactionId;    // Razorpay payment ID
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WorkerResponse {
        private Long    id;
        private String  workerUsername;
        private String  workerName;
        private String  workerMobile;
        private String  ownerUsername;
        private boolean canCreateInvoice;
        private boolean canEditInvoice;
        private boolean canDeleteInvoice;
        private boolean canViewReports;
        private boolean canManageStock;
        private boolean canViewProducts;
        private boolean canAddProduct;
        private boolean canEditProduct;
        private boolean canDeleteProduct;
        private boolean isPaidSlot;
        private boolean isActive;
        // Plan info
        private String  workerPlanId;
        private String  workerPlanLabel;
        private String  workerPlanStart;
        private String  workerPlanEnd;
        private int     workerPlanDaysLeft;
        private boolean workerPlanActive;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WorkerBillResponse {
        private int        totalWorkers;
        private int        freeWorkers;
        private int        paidWorkers;
        private int        pricePerWorker;
        private BigDecimal totalAmount;
    }

    // ══════════════════════════════════════════════════════
    //  REPORT
    // ══════════════════════════════════════════════════════
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReportRequest {
        @NotBlank private String fromDate;
        @NotBlank private String toDate;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SalesReportResponse {
        private String fromDate;
        private String toDate;
        private int totalInvoices;
        private BigDecimal totalRevenue;
        private BigDecimal totalGstCollected;
        private BigDecimal totalProfit;
        private BigDecimal cashSales;
        private BigDecimal upiSales;
        private BigDecimal cardSales;
        private BigDecimal creditSales;
    }

    // ══════════════════════════════════════════════════════
    //  DASHBOARD
    // ══════════════════════════════════════════════════════
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardResponse {
        private BigDecimal todaySales;
        private int    todayInvoices;
        private int    totalProducts;
        private int    lowStockCount;
        private int    totalWorkers;
        private int    unreadAlerts;
        private String storeName;
        private String ownerName;
        private String planId;
        private String planLabel;
        private String planStart;
        private String planExpiry;
        private int    planDaysLeft;
        private boolean planActive;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkerPlanRequest {
        private String planId;
        private String razorpayPaymentId;
        private Long   workerSlotId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DeleteRequest {
        private String reason;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkerPlanPricing {
        private int    monthly;
        private int    halfYearly;
        private int    yearly;
        private String labelMonthly;
        private String labelHalfYearly;
        private String labelYearly;
        private int    freeSlots;
    }
}