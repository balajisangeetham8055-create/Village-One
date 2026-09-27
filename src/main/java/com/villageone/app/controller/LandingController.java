package com.villageone.app.controller;

import com.villageone.app.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/landing")
@RequiredArgsConstructor
public class LandingController {

    private final AppConfigRepository configRepo;

    @GetMapping("/social")
    public ResponseEntity<?> getSocialLinks() {
        String instagram = configRepo.findByKeyName("social_instagram")
                .map(c -> c.getKeyValue()).orElse("#");
        String twitter   = configRepo.findByKeyName("social_twitter")
                .map(c -> c.getKeyValue()).orElse("#");
        String whatsapp  = configRepo.findByKeyName("social_whatsapp")
                .map(c -> c.getKeyValue()).orElse("#");

        return ResponseEntity.ok(List.of(
                Map.of(
                        "name",   "Instagram",
                        "href",   instagram,
                        "color",  "#E1306C",
                        "bg",     "#fce4ec",
                        "icon",   "https://cdn.jsdelivr.net/npm/simple-icons@v9/icons/instagram.svg",
                        "filter", "invert(29%) sepia(89%) saturate(1234%) hue-rotate(317deg) brightness(95%) contrast(96%)"
                ),
                Map.of(
                        "name",   "X (Twitter)",
                        "href",   twitter,
                        "color",  "#000000",
                        "bg",     "#f3f4f6",
                        "icon",   "https://cdn.jsdelivr.net/npm/simple-icons@v9/icons/x.svg",
                        "filter", "invert(0%) sepia(0%) saturate(0%) hue-rotate(0deg) brightness(0%) contrast(100%)"
                ),
                Map.of(
                        "name",   "WhatsApp",
                        "href",   whatsapp,
                        "color",  "#25D366",
                        "bg",     "#e8f5e9",
                        "icon",   "https://cdn.jsdelivr.net/npm/simple-icons@v9/icons/whatsapp.svg",
                        "filter", "invert(64%) sepia(61%) saturate(456%) hue-rotate(95deg) brightness(95%) contrast(92%)"
                )
        ));
    }

    @GetMapping
    public ResponseEntity<?> getLanding() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("heroTitle",    "VillageOne Invoicing Software");
        c.put("heroSubtitle", "Create professional bills in seconds.");
        c.put("heroCta",      "Get Started");
        c.put("storeTypes",   List.of("Grocery Store","Electric Shop","General Store","Hardware Store","Medical Store","Restaurant"));
        c.put("features", List.of(
                Map.of("icon","📋","label","GST Ready Invoices"),
                Map.of("icon","📦","label","Inventory Management"),
                Map.of("icon","💰","label","Profit Tracking"),
                Map.of("icon","📄","label","Download PDF Bills"),
                Map.of("icon","🔔","label","Stock Alerts"),
                Map.of("icon","✅","label","Easy to Use")
        ));
        c.put("trustBadge","Trusted by thousands of small businesses. 100% safe and secure.");
        return ResponseEntity.ok(c);
    }
}