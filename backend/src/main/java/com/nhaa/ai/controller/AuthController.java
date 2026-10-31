package com.nhaa.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email != null && !email.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.demo_token",
                    "user", Map.of(
                            "id", "ADMIN_01",
                            "name", "NHAA Authorized Administrator",
                            "email", email,
                            "role", "ADMINISTRATOR"
                    ),
                    "status", "authenticated"
            ));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials."));
    }
}
