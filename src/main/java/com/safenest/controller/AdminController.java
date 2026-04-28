package com.safenest.controller;

import com.safenest.service.PlatformService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PlatformService platformService;

    public AdminController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return platformService.getUsers();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Integer id) {
        Map<String, Object> user = platformService.getUserById(id);
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Integer id) {
        return platformService.deleteUser(id)
            ? ResponseEntity.ok(Map.of("message", "User " + id + " deleted successfully"))
            : ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    public Map<String, Integer> getStats() {
        return platformService.getSystemStats();
    }

    @GetMapping("/activities")
    public List<Map<String, Object>> getActivities() {
        return platformService.getRecentActivities();
    }
}
