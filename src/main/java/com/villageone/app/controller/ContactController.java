package com.villageone.app.controller;

import com.villageone.app.repository.ContactInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactInfoRepository contactRepo;

    @GetMapping
    public ResponseEntity<?> getContact() {
        return contactRepo.findFirstBy()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}