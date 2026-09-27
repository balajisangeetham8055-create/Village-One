package com.villageone.app.controller;

import com.villageone.app.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepo;

    @GetMapping
    public ResponseEntity<?> getPlans() {
        return ResponseEntity.ok(planRepo.findByActiveTrue());
    }
}