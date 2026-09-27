package com.villageone.app.controller;

import com.villageone.app.entity.*;
import com.villageone.app.repository.*;
import com.villageone.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository              userRepo;
    private final WorkerSlotRepository        workerRepo;
    private final InvoiceRepository           invoiceRepo;
    private final SupportTicketRepository     ticketRepo;
    private final InvoiceItemRepository       itemRepo;
    private final AppConfigRepository         configRepo;
    private final JwtUtil                     jwtUtil;
    private final PasswordEncoder             passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        log.info("Admin login attempt: username={}", username);

        String adminUser = configRepo.findByKeyName("admin_username").map(AppConfig::getKeyValue).orElse(null);
        String adminHashedPw = configRepo.findByKeyName("admin_password_hash").map(AppConfig::getKeyValue).orElse(null);

        if (adminUser == null || adminHashedPw == null)
            return ResponseEntity.status(503).body(Map.of("message", "Admin not configured. Run setup first."));

        if (!adminHashedPw.startsWith("$2a$") && !adminHashedPw.startsWith("$2b$")) {
            log.error("Invalid hash format: {}", adminHashedPw.substring(0, Math.min(15, adminHashedPw.length())));
            return ResponseEntity.status(503).body(Map.of("message", "Admin password hash invalid. Use /api/admin/setup-password to fix."));
        }

        if (!username.equals(adminUser) || !passwordEncoder.matches(password, adminHashedPw))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid admin credentials"));

        String token = jwtUtil.generateToken(username, "ADMIN", -1L);
        log.info("Admin login successful: username={}", username);
        return ResponseEntity.ok(Map.of("token", token, "role", "ADMIN", "username", username));
    }

    // ONE-TIME setup endpoint to hash and save admin password properly
    @PostMapping("/setup-password")
    public ResponseEntity<?> setupPassword(@RequestBody Map<String, String> body) {
        String setupKey = body.get("setupKey");
        String password = body.get("password");
        String username = body.getOrDefault("username", "admin");

        String expectedKey = configRepo.findByKeyName("admin_setup_key")
                .map(AppConfig::getKeyValue).orElse("VILLAGEONE_SETUP_2025");

        if (!expectedKey.equals(setupKey))
            return ResponseEntity.status(403).body(Map.of("message", "Invalid setup key"));
        if (password == null || password.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "Password min 6 characters"));

        String hash = passwordEncoder.encode(password);
        saveConfig("admin_username", username, "Super admin login username");
        saveConfig("admin_password_hash", hash, "BCrypt hashed admin password");

        log.info("Admin password set for username={}", username);
        return ResponseEntity.ok(Map.of("message", "Admin password set!", "username", username, "hashOk", hash.startsWith("$2a$")));
    }

    private void saveConfig(String key, String value, String description) {
        AppConfig c = configRepo.findByKeyName(key)
                .orElse(AppConfig.builder().keyName(key).description(description).build());
        c.setKeyValue(value); configRepo.save(c);
    }

    private boolean isAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.isTokenValid(token) && "ADMIN".equals(jwtUtil.extractRole(token));
    }
    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(403).body(Map.of("message","Admin access required")); }

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(@RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        List<User> businesses = userRepo.findAll().stream().filter(u -> u.getRole() == User.Role.BUSINESS).collect(Collectors.toList());
        List<WorkerSlot> workers = workerRepo.findAll();
        List<SupportTicket> tickets = ticketRepo.findAll();
        List<Invoice> invoices = invoiceRepo.findAll();
        long activeBusinesses = businesses.stream().filter(u -> u.getPlanEnd() != null && u.getPlanEnd().isAfter(LocalDate.now())).count();
        long expiredPlans = businesses.stream().filter(u -> u.getPlanEnd() != null && u.getPlanEnd().isBefore(LocalDate.now())).count();
        long activeWorkers = workers.stream().filter(w -> w.isActive() && (w.getWorkerPlanEnd() == null || w.getWorkerPlanEnd().isAfter(LocalDate.now()))).count();
        long openTickets = tickets.stream().filter(t -> "OPEN".equals(t.getStatus())).count();
        BigDecimal totalRevenue = invoices.stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> recentUsers = businesses.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.reverseOrder())).limit(10)
                .map(u -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",u.getId()); m.put("fullName",u.getFullName()); m.put("storeName",u.getStoreName()); m.put("mobile",u.getMobile()); m.put("planId",u.getPlanId()); m.put("planEnd",u.getPlanEnd()!=null?u.getPlanEnd().toString():null); m.put("createdAt",u.getCreatedAt().toString()); return m; }).collect(Collectors.toList());
        Map<String,Object> overview = new LinkedHashMap<>();
        overview.put("totalBusinesses",businesses.size()); overview.put("activeBusinesses",activeBusinesses);
        overview.put("totalWorkers",workers.size()); overview.put("activeWorkers",activeWorkers);
        overview.put("totalInvoices",invoices.size()); overview.put("totalRevenue",totalRevenue);
        overview.put("activePlans",businesses.size()-expiredPlans); overview.put("expiredPlans",expiredPlans);
        overview.put("totalTickets",tickets.size()); overview.put("openTickets",openTickets);
        overview.put("recentUsers",recentUsers);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/businesses")
    public ResponseEntity<?> getBusinesses(@RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        return ResponseEntity.ok(userRepo.findAll().stream().filter(u -> u.getRole() == User.Role.BUSINESS)
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.reverseOrder()))
                .map(u -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",u.getId()); m.put("fullName",u.getFullName()); m.put("username",u.getUsername()); m.put("mobile",u.getMobile()); m.put("email",u.getEmail()); m.put("storeName",u.getStoreName()); m.put("storeType",u.getStoreType()); m.put("storeAddress",u.getStoreAddress()); m.put("city",u.getCity()); m.put("state",u.getState()); m.put("storePincode",u.getStorePincode()); m.put("gstNumber",u.getGstNumber()); m.put("planId",u.getPlanId()); m.put("planStart",u.getPlanStart()!=null?u.getPlanStart().toString():null); m.put("planEnd",u.getPlanEnd()!=null?u.getPlanEnd().toString():null); m.put("transactionId",u.getTransactionId()); m.put("createdAt",u.getCreatedAt().toString()); return m; })
                .collect(Collectors.toList()));
    }

    @GetMapping("/businesses/{id}/workers")
    public ResponseEntity<?> getBusinessWorkers(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        User u = userRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(workerRepo.findByOwnerUsernameAndIsActiveTrue(u.getUsername()));
    }

    @GetMapping("/businesses/{id}/stats")
    public ResponseEntity<?> getBusinessStats(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        User u = userRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        List<Invoice> invs = invoiceRepo.findByOwnerUsernameOrderByCreatedAtDesc(u.getUsername());
        BigDecimal rev = invs.stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return ResponseEntity.ok(Map.of("invoiceCount", invs.size(), "totalRevenue", rev));
    }

    @GetMapping("/workers")
    public ResponseEntity<?> getAllWorkers(@RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        return ResponseEntity.ok(workerRepo.findAll().stream()
                .sorted(Comparator.comparing(WorkerSlot::getCreatedAt, Comparator.reverseOrder()))
                .map(w -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",w.getId()); m.put("workerName",w.getWorkerName()); m.put("workerUsername",w.getWorkerUsername()); m.put("workerMobile",w.getWorkerMobile()); m.put("ownerUsername",w.getOwnerUsername()); m.put("isPaidSlot",w.isPaidSlot()); m.put("isActive",w.isActive()); m.put("workerPlanId",w.getWorkerPlanId()); m.put("workerPlanLabel",w.getWorkerPlanLabel()); m.put("workerPlanEnd",w.getWorkerPlanEnd()!=null?w.getWorkerPlanEnd().toString():null); m.put("createdAt",w.getCreatedAt().toString()); return m; })
                .collect(Collectors.toList()));
    }

    @GetMapping("/tickets")
    public ResponseEntity<?> getTickets(@RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        return ResponseEntity.ok(ticketRepo.findAll().stream().sorted(Comparator.comparing(SupportTicket::getCreatedAt, Comparator.reverseOrder())).collect(Collectors.toList()));
    }

    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<?> updateTicketStatus(@PathVariable String ticketId, @RequestBody Map<String,String> body, @RequestHeader("Authorization") String auth) {
        if (!isAdmin(auth)) return unauthorized();
        return ticketRepo.findByTicketId(ticketId).map(t -> { t.setStatus(body.get("status")); ticketRepo.save(t); return ResponseEntity.ok(Map.of("message","Status updated")); }).orElse(ResponseEntity.notFound().build());
    }
}