package com.freshcut.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        AuthResponse res = authService.register(req);
        // Emitir cookie HttpOnly con el token para navegación segura
        if (res.getToken() != null && !res.getToken().isBlank()) {
            ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", res.getToken())
                    .httpOnly(true).secure(true).sameSite("None").path("/").build();
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(res);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        if (res.getToken() != null && !res.getToken().isBlank()) {
            ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", res.getToken())
                    .httpOnly(true).secure(true).sameSite("None").path("/").build();
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(res);
        }
        return ResponseEntity.ok(res);
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
    public ResponseEntity<AuthResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = auth.getName();
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        return ResponseEntity.ok(new AuthResponse("", email, role));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Expirar cookie HttpOnly
        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", "")
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }
}