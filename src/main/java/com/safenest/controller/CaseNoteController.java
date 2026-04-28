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
@RequestMapping("/api/casenotes")
public class CaseNoteController {

    private final PlatformService platformService;

    public CaseNoteController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/counsellor/{counsellorId}")
    public List<Map<String, Object>> getCounsellorCaseNotes(@PathVariable Integer counsellorId) {
        return platformService.getCounsellorCaseNotes(counsellorId);
    }

    @GetMapping("/victim/{victimId}")
    public List<Map<String, Object>> getVictimCaseNotes(@PathVariable Integer victimId) {
        return platformService.getVictimCaseNotes(victimId);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCaseNote(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createCaseNote(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCaseNote(@PathVariable Integer id) {
        return platformService.deleteCaseNote(id)
            ? ResponseEntity.ok(Map.of("message", "Case note deleted"))
            : ResponseEntity.notFound().build();
    }
}
