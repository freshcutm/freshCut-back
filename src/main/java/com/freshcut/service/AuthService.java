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
import com.freshcut.dto.AuthDtos.ResetSimpleRequest;
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
        if (!isStrongPassword(req.getPassword())) {
            throw new IllegalArgumentException("Contraseña inválida: mínimo 8 caracteres, incluir mayúscula, minúscula, número y carácter especial");
        }
        Role role = "ADMIN".equalsIgnoreCase(req.getRole()) ? Role.ADMIN :
                ("BARBER".equalsIgnoreCase(req.getRole()) ? Role.BARBER : Role.USER);
        User u = new User();
        u.setEmail(req.getEmail());
        u.setPasswordHash(passwordEncoder.encode(sha256(req.getPassword())));
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
        String email = (req != null && req.getEmail() != null) ? req.getEmail().trim() : "";
        String password = (req != null && req.getPassword() != null) ? req.getPassword() : "";
        String passwordSha = (req != null && req.getPasswordSha256() != null) ? req.getPasswordSha256() : "";

        // Si faltan datos básicos, responder 200 con token vacío para evitar errores en consola
        if (email.isBlank() || (password.isBlank() && passwordSha.isBlank())) {
            return new AuthResponse("", email, "");
        }

        var opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            // Responder 200 OK con token vacío para evitar errores de consola
            return new AuthResponse("", email, "");
        }
        User u = opt.get();

        boolean ok = false;
        if (!password.isBlank()) {
            ok = passwordEncoder.matches(password, u.getPasswordHash());
            if (!ok) {
                // Compatibilidad con cuentas creadas cuando el frontend enviaba SHA-256 del password
                String legacy = sha256(password);
                ok = passwordEncoder.matches(legacy, u.getPasswordHash());
            }
        }

        // Si no se validó con el texto plano (o no se envió), intentar con el hash enviado
        if (!ok && !passwordSha.isBlank()) {
            ok = passwordEncoder.matches(passwordSha, u.getPasswordHash());
        }

        if (!ok) {
            // Responder 200 OK con token vacío para evitar errores de consola
            return new AuthResponse("", email, "");
        }

        String token = jwtService.generate(u.getEmail(), Map.of("role", u.getRole().name()));
        return new AuthResponse(token, u.getEmail(), u.getRole().name());
    }

    private String sha256(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return text; // fallback (no debería ocurrir)
        }
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
        if (!isStrongPassword(req.getNewPassword())) {
            throw new IllegalArgumentException("Contraseña inválida: mínimo 8 caracteres, incluir mayúscula, minúscula, número y carácter especial");
        }
        u.setPasswordHash(passwordEncoder.encode(sha256(req.getNewPassword())));
        u.setResetCode(null);
        u.setResetExpiry(null);
        userRepository.save(u);
    }

    // Reset simple: solo email existente y nueva contraseña válida (sin código)
    public void resetPasswordSimple(ResetSimpleRequest req) {
        String email = (req != null && req.getEmail() != null) ? req.getEmail().trim().toLowerCase() : "";
        String newPw = (req != null && req.getNewPassword() != null) ? req.getNewPassword() : "";
        if (email.isBlank() || newPw.isBlank()) {
            throw new IllegalArgumentException("Email y nueva contraseña son requeridos");
        }
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (!isStrongPassword(newPw)) {
            throw new IllegalArgumentException("Contraseña inválida: mínimo 8 caracteres, incluir mayúscula, minúscula, número y carácter especial");
        }
        u.setPasswordHash(passwordEncoder.encode(sha256(newPw)));
        // Limpiar cualquier código previo de reset
        u.setResetCode(null);
        u.setResetExpiry(null);
        userRepository.save(u);
    }

    private boolean isStrongPassword(String pw) {
        if (pw == null) return false;
        return pw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    }
}