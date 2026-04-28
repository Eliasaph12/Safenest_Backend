package com.safenest.controller;

import com.safenest.service.PlatformService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PlatformService platformService;

    public AuthController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = platformService.requestLoginOtp(
            String.valueOf(request.get("email")),
            String.valueOf(request.get("password")),
            request.get("phoneNumber") != null ? String.valueOf(request.get("phoneNumber")) : null
        );
        if (response.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = platformService.requestRegistrationOtp(request);
        if (response.containsKey("error")) {
            HttpStatus status = "Email already registered".equals(response.get("error")) || "Phone number already registered".equals(response.get("error"))
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup/verify-otp")
    public ResponseEntity<Map<String, Object>> verifySignupOtp(@RequestBody Map<String, Object> request) {
        Long verificationId = request.get("verificationId") == null ? null : Long.valueOf(String.valueOf(request.get("verificationId")));
        Map<String, Object> response = platformService.verifyRegistrationOtp(verificationId, String.valueOf(request.get("otp")));
        if (response.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyLoginOtp(@RequestBody Map<String, Object> request) {
        Long verificationId = request.get("verificationId") == null ? null : Long.valueOf(String.valueOf(request.get("verificationId")));
        Map<String, Object> response = platformService.verifyLoginOtp(verificationId, String.valueOf(request.get("otp")));
        if (response.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return platformService.health();
    }
}
