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
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final PlatformService platformService;

    public AppointmentController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/counsellor/{counsellorId}")
    public List<Map<String, Object>> getCounsellorAppointments(@PathVariable Integer counsellorId) {
        return platformService.getCounsellorAppointments(counsellorId);
    }

    @GetMapping("/victim/{victimId}")
    public List<Map<String, Object>> getVictimAppointments(@PathVariable Integer victimId) {
        return platformService.getVictimAppointments(victimId);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAppointment(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createAppointment(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAppointment(@PathVariable Integer id) {
        return platformService.deleteAppointment(id)
            ? ResponseEntity.ok(Map.of("message", "Appointment deleted"))
            : ResponseEntity.notFound().build();
    }
}
