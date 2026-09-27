package com.villageone.app.controller;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.ContactInfo;
import com.villageone.app.entity.SupportTicket;
import com.villageone.app.repository.ContactInfoRepository;
import com.villageone.app.repository.SupportTicketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportTicketRepository ticketRepo;
    private final ContactInfoRepository   contactRepo;

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(List.of(
                Map.of("id","billing", "label","Billing Issue",     "icon","💳"),
                Map.of("id","invoice", "label","Invoice Problem",   "icon","🧾"),
                Map.of("id","stock",   "label","Stock / Inventory", "icon","📦"),
                Map.of("id","login",   "label","Login Problem",     "icon","🔐"),
                Map.of("id","plan",    "label","Plan / Payment",    "icon","📋"),
                Map.of("id","worker",  "label","Worker Access",     "icon","👷"),
                Map.of("id","pdf",     "label","PDF / Print",       "icon","📄"),
                Map.of("id","data",    "label","Data Issue",        "icon","💾"),
                Map.of("id","speed",   "label","App is Slow",       "icon","🐢"),
                Map.of("id","other",   "label","Other",             "icon","❓")
        ));
    }

    @PostMapping("/ticket")
    public ResponseEntity<?> raiseTicket(@Valid @RequestBody TicketRequest req) {
        try {
            String ticketId;
            do { ticketId = "VIL-" + (10000 + new Random().nextInt(89999)); }
            while (ticketRepo.existsByTicketId(ticketId));

            SupportTicket ticket = SupportTicket.builder()
                    .ticketId(ticketId)
                    .issueType(req.getIssueType())
                    .priority(req.getPriority())
                    .name(req.getName())
                    .mobile(req.getMobile())
                    .email(req.getEmail())
                    .subject(req.getSubject())
                    .description(req.getDescription())
                    .build();
            ticketRepo.save(ticket);

            String wa = contactRepo.findFirstBy().map(ContactInfo::getWhatsapp).orElse("9999999999");
            String waLink = "https://wa.me/91" + wa + "?text=Hi%2C%20my%20support%20ticket%20ID%20is%20" + ticketId;

            return ResponseEntity.ok(new TicketResponse(
                    ticketId, "Ticket raised! Our team will contact you within 24 hours.", waLink
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message","Could not raise ticket. Please try again."));
        }
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<?> getTicketStatus(@PathVariable String ticketId) {
        return ticketRepo.findByTicketId(ticketId)
                .map(t -> ResponseEntity.ok(Map.of(
                        "ticketId",  t.getTicketId(),
                        "status",    t.getStatus(),
                        "subject",   t.getSubject(),
                        "issueType", t.getIssueType(),
                        "createdAt", t.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}