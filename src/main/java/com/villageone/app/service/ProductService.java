package com.villageone.app.service;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.Product;
import com.villageone.app.entity.StockAlert;
import com.villageone.app.repository.ProductRepository;
import com.villageone.app.repository.StockAlertRepository;
import com.villageone.app.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository   productRepo;
    private final StockAlertRepository alertRepo;

    public ProductResponse addProduct(String ownerUsername, ProductRequest req) {
        if (productRepo.existsByOwnerUsernameAndName(ownerUsername, req.getName()))
            throw new RuntimeException("Product '" + req.getName() + "' already exists");

        Long businessId = JwtAuthFilter.BUSINESS_ID.get();
        Product p = Product.builder()
                .businessId(businessId)
                .ownerUsername(ownerUsername)
                .name(req.getName())
                .shortCode(req.getShortCode())
                .category(req.getCategory())
                .unit(req.getUnit())
                .purchasePrice(req.getPurchasePrice())
                .sellingPrice(req.getSellingPrice())
                .gstPercent(req.getGstPercent())
                .stockQuantity(req.getStockQuantity())
                .lowStockThreshold(req.getLowStockThreshold())
                .build();

        Product saved = productRepo.save(p);
        checkAndCreateAlert(saved, ownerUsername);
        return toResponse(saved);
    }

    public List<ProductResponse> getProducts(String ownerUsername) {
        return productRepo.findByOwnerUsername(ownerUsername).stream().map(this::toResponse).toList();
    }

    public ProductResponse updateProduct(Long id, String ownerUsername, ProductRequest req) {
        Product p = productRepo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        if (!p.getOwnerUsername().equals(ownerUsername)) throw new RuntimeException("Access denied");

        p.setName(req.getName()); p.setShortCode(req.getShortCode());
        p.setCategory(req.getCategory()); p.setUnit(req.getUnit());
        p.setPurchasePrice(req.getPurchasePrice()); p.setSellingPrice(req.getSellingPrice());
        p.setGstPercent(req.getGstPercent()); p.setStockQuantity(req.getStockQuantity());
        p.setLowStockThreshold(req.getLowStockThreshold());

        Product saved = productRepo.save(p);
        checkAndCreateAlert(saved, ownerUsername);
        return toResponse(saved);
    }

    public ProductResponse addStock(String ownerUsername, StockUpdateRequest req) {
        Product p = productRepo.findById(req.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
        if (!p.getOwnerUsername().equals(ownerUsername)) throw new RuntimeException("Access denied");
        p.setStockQuantity(Math.round((p.getStockQuantity() + req.getAddQuantity()) * 1000.0) / 1000.0);
        return toResponse(productRepo.save(p));
    }

    public void deleteProduct(Long id, String ownerUsername) {
        Product p = productRepo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        if (!p.getOwnerUsername().equals(ownerUsername)) throw new RuntimeException("Access denied");
        productRepo.delete(p);
    }

    public void deductStock(Long productId, BigDecimal quantity) {
        productRepo.findById(productId).ifPresent(p -> {
            // Double supports 0.5kg, 0.25kg etc
            double newQty = Math.max(0.0, p.getStockQuantity() - quantity.doubleValue());
            // Round to 3 decimal places
            newQty = Math.round(newQty * 1000.0) / 1000.0;
            p.setStockQuantity(newQty);
            Product saved = productRepo.save(p);
            checkAndCreateAlert(saved, p.getOwnerUsername());
        });
    }

    private void checkAndCreateAlert(Product p, String ownerUsername) {
        // Auto-dismiss existing alert if stock is now above threshold
        if (p.getStockQuantity() > p.getLowStockThreshold()) {
            alertRepo.findByOwnerUsernameAndProductIdAndIsReadFalse(ownerUsername, p.getId())
                    .ifPresent(a -> { a.setRead(true); alertRepo.save(a); });
            return;
        }
        if (p.getStockQuantity() <= p.getLowStockThreshold()) {
            boolean exists = alertRepo.existsByOwnerUsernameAndProductIdAndIsReadFalse(ownerUsername, p.getId());
            if (!exists) {
                alertRepo.save(StockAlert.builder()
                        .businessId(p.getBusinessId())
                        .ownerUsername(ownerUsername)
                        .productId(p.getId())
                        .productName(p.getName() + " (" + p.getUnit() + ")")  // include unit
                        .currentStock(p.getStockQuantity().intValue())
                        .threshold(p.getLowStockThreshold().intValue())
                        .build());
            }
        }
    }

    public ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId()).name(p.getName()).shortCode(p.getShortCode())
                .category(p.getCategory()).unit(p.getUnit())
                .purchasePrice(p.getPurchasePrice()).sellingPrice(p.getSellingPrice())
                .gstPercent(p.getGstPercent()).stockQuantity(p.getStockQuantity())
                .lowStockThreshold(p.getLowStockThreshold())
                .lowStock(p.getStockQuantity() <= p.getLowStockThreshold())
                .build();
    }
}