package com.freshcut.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.freshcut.dto.AuthDtos.AuthResponse;
import com.freshcut.dto.AuthDtos.LoginRequest;
import com.freshcut.dto.AuthDtos.RegisterRequest;
import com.freshcut.dto.AuthDtos.ForgotRequest;
import com.freshcut.dto.AuthDtos.ResetRequest;
import com.freshcut.dto.AuthDtos.ResetSimpleRequest;
import com.freshcut.security.JwtService;
import com.freshcut.service.AuthService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotRequest req) {
        authService.requestPasswordReset(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-simple")
    public ResponseEntity<Void> resetSimple(@Valid @RequestBody ResetSimpleRequest req) {
        authService.resetPasswordSimple(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        Claims claims = jwtService.parse(authorization.substring(7));
        String email = claims.getSubject();
        String role = (String) claims.get("role");
        return ResponseEntity.ok(new AuthResponse("", email, role));
    }
}