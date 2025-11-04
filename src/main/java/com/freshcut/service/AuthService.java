package com.freshcut.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.freshcut.dto.AuthDtos.ForgotRequest;
import com.freshcut.dto.AuthDtos.AuthResponse;
import com.freshcut.dto.AuthDtos.LoginRequest;
import com.freshcut.dto.AuthDtos.RegisterRequest;
import com.freshcut.dto.AuthDtos.ResetRequest;
import com.freshcut.db.model.Barber;
import com.freshcut.db.model.Role;
import com.freshcut.db.model.User;
import com.freshcut.db.repository.BarberRepository;
import com.freshcut.db.repository.UserRepository;
import com.freshcut.security.JwtService;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BarberRepository barberRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, BarberRepository barberRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.barberRepository = barberRepository;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        Role role = "ADMIN".equalsIgnoreCase(req.getRole()) ? Role.ADMIN :
                ("BARBER".equalsIgnoreCase(req.getRole()) ? Role.BARBER : Role.USER);
        User u = new User();
u.setEmail(req.getEmail());
u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
u.setRole(role);
u.setName(req.getName());
        if (role == Role.BARBER) {
            // Si viene barberId se asocia, si no, se crea un perfil nuevo de barbero con el nombre proporcionado
            if (req.getBarberId() != null && !req.getBarberId().isBlank()) {
                u.setBarberId(req.getBarberId());
            } else {
                if (req.getName() == null || req.getName().isBlank()) {
                    throw new IllegalArgumentException("Para rol BARBER debes indicar nombre para crear tu perfil");
                }
                Barber b = new Barber();
                b.setName(req.getName());
                b.setActive(true);
                Barber saved = barberRepository.save(b);
                u.setBarberId(saved.getId());
            }
        }
        userRepository.save(u);
        String token = jwtService.generate(u.getEmail(), Map.of("role", u.getRole().name()));
        return new AuthResponse(token, u.getEmail(), u.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        String token = jwtService.generate(u.getEmail(), Map.of("role", u.getRole().name()));
        return new AuthResponse(token, u.getEmail(), u.getRole().name());
    }

    // Solicitar recuperación: genera código de 6 dígitos y lo guarda con expiración
    public void requestPasswordReset(ForgotRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(u -> {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            u.setResetCode(code);
            u.setResetExpiry(Instant.now().plus(Duration.ofMinutes(15)));
            userRepository.save(u);
            // Enviar email (por ahora, log). Se puede integrar JavaMailSender luego.
            log.info("[PasswordReset] Enviar código {} a {} (expira en 15 min)", code, u.getEmail());
        });
        // Respuesta siempre genérica para no filtrar si existe o no el email
    }

    // Confirmar recuperación: valida código y establece nueva contraseña
    public void resetPassword(ResetRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (u.getResetCode() == null || u.getResetExpiry() == null) {
            throw new IllegalArgumentException("Código no solicitado");
        }
        if (Instant.now().isAfter(u.getResetExpiry())) {
            throw new IllegalArgumentException("Código expirado");
        }
        if (!req.getCode().equals(u.getResetCode())) {
            throw new IllegalArgumentException("Código inválido");
        }
        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        u.setResetCode(null);
        u.setResetExpiry(null);
        userRepository.save(u);
    }
}