package com.safenest.controller;

import com.safenest.service.PlatformService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/legalcases")
public class LegalCaseController {

    private final PlatformService platformService;

    public LegalCaseController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/advisor/{advisorId}")
    public List<Map<String, Object>> getAdvisorCases(@PathVariable Integer advisorId) {
        return platformService.getAdvisorCases(advisorId);
    }

    @GetMapping("/victim/{victimId}")
    public List<Map<String, Object>> getVictimCases(@PathVariable Integer victimId) {
        return platformService.getVictimCases(victimId);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createLegalCase(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createLegalCase(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteLegalCase(@PathVariable Integer id) {
        return platformService.deleteLegalCase(id)
            ? ResponseEntity.ok(Map.of("message", "Legal case deleted"))
            : ResponseEntity.notFound().build();
    }
}
