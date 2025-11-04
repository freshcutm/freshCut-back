package com.freshcut.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshcut.db.model.User;
import com.freshcut.db.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {
    private final UserRepository userRepo;

    public AdminUserController(UserRepository userRepo) { this.userRepo = userRepo; }

    public static class UserDto {
        public String id;
        public String email;
        public String role;
        public String name;
        public String barberId;
        public java.time.Instant createdAt;
        public UserDto(User u) {
            this.id = u.getId();
            this.email = u.getEmail();
            this.role = u.getRole() != null ? u.getRole().name() : null;
            this.name = u.getName();
            this.barberId = u.getBarberId();
            this.createdAt = u.getCreatedAt();
        }
    }

    @GetMapping
    public List<UserDto> list() {
        return userRepo.findAll().stream().map(UserDto::new).collect(Collectors.toList());
    }

    // Obtener un usuario por email (ADMIN)
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserDto> getByEmail(@PathVariable String email) {
        return userRepo.findByEmail(email.toLowerCase())
                .map(u -> ResponseEntity.ok(new UserDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        // Evitar que un admin se borre a sí mismo accidentalmente
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? (String) auth.getPrincipal() : null;
        if (email != null) {
            var me = userRepo.findByEmail(email).orElse(null);
            if (me != null && id.equals(me.getId())) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Eliminar por email (ADMIN)
    @DeleteMapping("/by-email/{email}")
    public ResponseEntity<Void> deleteByEmail(@PathVariable String email) {
        // Evitar auto-eliminación
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = (auth != null) ? (String) auth.getPrincipal() : null;
        if (currentEmail != null && currentEmail.equalsIgnoreCase(email)) {
            return ResponseEntity.badRequest().build();
        }
        var opt = userRepo.findByEmail(email.toLowerCase());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        userRepo.deleteById(opt.get().getId());
        return ResponseEntity.noContent().build();
    }
}