package com.villageone.app.controller;

import com.villageone.app.entity.User;
import com.villageone.app.repository.UserRepository;
import com.villageone.app.repository.WorkerSlotRepository;
import com.villageone.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final WorkerSlotRepository workerSlotRepo;
    private final UserRepository userRepo;
    private final JwtUtil        jwtUtil;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String auth) {
        try {
            String username = jwtUtil.extractUsername(auth.replace("Bearer ", ""));
            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Map<String, String> r = new LinkedHashMap<>();
            r.put("username",     user.getUsername());
            r.put("fullName",     user.getFullName());
            r.put("mobile",       user.getMobile());
            r.put("email",        user.getEmail());
            r.put("address",      user.getAddress());
            r.put("pincode",      user.getPincode());
            r.put("storeName",    user.getStoreName());
            r.put("storeType",    user.getStoreType());
            r.put("storeAddress", user.getStoreAddress());
            r.put("city",         user.getCity());
            r.put("state",        user.getState());
            r.put("storePincode", user.getStorePincode());
            r.put("gstNumber",    user.getGstNumber()  != null ? user.getGstNumber()            : "");
            r.put("planId",       user.getPlanId()     != null ? user.getPlanId()               : "");
            r.put("planStart",    user.getPlanStart()  != null ? user.getPlanStart().toString() : "");
            r.put("planEnd",      user.getPlanEnd()    != null ? user.getPlanEnd().toString()   : "");
            r.put("role",         user.getRole().name());
            r.put("profilePhoto", user.getProfilePhoto() != null ? user.getProfilePhoto() : "");
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    // ── PUT /api/user/photo — save base64 profile photo ──
    @PutMapping("/photo")
    public ResponseEntity<?> updatePhoto(
            @RequestHeader("Authorization") String auth,
            @RequestBody Map<String, String> body) {
        try {
            String username = jwtUtil.extractUsername(auth.replace("Bearer ", ""));
            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String photo = body.get("photo");
            if (photo == null || photo.isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Photo data required"));
            // Store base64 photo - max 500KB (base64 ~667KB for 500KB image)
            if (photo.length() > 700000)
                return ResponseEntity.badRequest().body(Map.of("message", "Photo too large. Max 500KB."));
            user.setProfilePhoto(photo);
            userRepo.save(user);
            return ResponseEntity.ok(Map.of("message", "Photo updated", "photo", photo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestHeader("Authorization") String auth,
                                      @RequestBody Map<String, String> body) {
        try {
            String username = jwtUtil.extractUsername(auth.replace("Bearer ", ""));
            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (body.containsKey("fullName")     && !body.get("fullName").isBlank())     user.setFullName(body.get("fullName"));
            if (body.containsKey("storeName")    && !body.get("storeName").isBlank())    user.setStoreName(body.get("storeName"));
            if (body.containsKey("storeType")    && !body.get("storeType").isBlank())    user.setStoreType(body.get("storeType"));
            if (body.containsKey("storeAddress") && !body.get("storeAddress").isBlank()) user.setStoreAddress(body.get("storeAddress"));
            if (body.containsKey("city")         && !body.get("city").isBlank())         user.setCity(body.get("city"));
            if (body.containsKey("state")        && !body.get("state").isBlank())        user.setState(body.get("state"));
            if (body.containsKey("gstNumber"))   user.setGstNumber(body.get("gstNumber"));
            if (body.containsKey("address")      && !body.get("address").isBlank())      user.setAddress(body.get("address"));
            if (body.containsKey("profilePhoto")) user.setProfilePhoto(body.get("profilePhoto"));
            userRepo.save(user);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    // GET /api/user/owner-profile — returns the business OWNER's profile for a worker
// Worker uses this to get correct store name for invoice printing
    @GetMapping("/owner-profile")
    public ResponseEntity<?> getOwnerProfile(@RequestHeader("Authorization") String auth) {
        try {
            String workerUsername = jwtUtil.extractUsername(auth.replace("Bearer ", ""));
            var slot = workerSlotRepo.findByWorkerUsername(workerUsername).orElse(null);
            if (slot == null) return getMe(auth);

            User owner = userRepo.findByUsername(slot.getOwnerUsername())
                    .orElseThrow(() -> new RuntimeException("Owner not found"));

            boolean ownerPlanExpired = owner.getPlanEnd() != null
                    && owner.getPlanEnd().isBefore(LocalDate.now());

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("storeName",    owner.getStoreName());
            r.put("storeType",    owner.getStoreType());
            r.put("storeAddress", owner.getStoreAddress());
            r.put("city",         owner.getCity());
            r.put("state",        owner.getState());
            r.put("storePincode", owner.getStorePincode());
            r.put("mobile",       owner.getMobile());
            r.put("gstNumber",    owner.getGstNumber() != null ? owner.getGstNumber() : "");
            r.put("planExpired",  ownerPlanExpired);
            r.put("planEnd",      owner.getPlanEnd() != null ? owner.getPlanEnd().toString() : "");
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}