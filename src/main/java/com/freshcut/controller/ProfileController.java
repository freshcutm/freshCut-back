package com.freshcut.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.freshcut.db.model.User;
import com.freshcut.db.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {
    private final UserRepository userRepo;
    private final Path avatarDir;

    public ProfileController(UserRepository userRepo) {
        this.userRepo = userRepo;
        this.avatarDir = Paths.get("uploads", "avatars");
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(avatarDir);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? (String) auth.getPrincipal() : null;
        if (email == null) throw new IllegalStateException("No autenticado");
        return userRepo.findByEmail(email).orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    public static class UserProfileDto {
        public String id;
        public String email;
        public String role;
        public String name;
        public String avatarUrl;
        public UserProfileDto(User u) {
            this.id = u.getId();
            this.email = u.getEmail();
            this.role = u.getRole().name();
            this.name = u.getName();
            // Solo exponer avatarUrl si hay ruta y el archivo existe, para evitar 404 en el frontend
            String url = null;
            if (u.getAvatarPath() != null && !u.getAvatarPath().isBlank()) {
                Path p = Paths.get(u.getAvatarPath());
                if (Files.exists(p)) {
                    url = "/api/profile/avatar/" + u.getId();
                }
            }
            this.avatarUrl = url;
        }
    }

    public static class UpdateProfileDto {
        @Size(min = 1, max = 50)
        public String name;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me() {
        User u = currentUser();
        return ResponseEntity.ok(new UserProfileDto(u));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(@Valid @RequestBody UpdateProfileDto payload) {
        User u = currentUser();
        if (payload.name != null && !payload.name.isBlank()) {
            u.setName(payload.name);
        }
        userRepo.save(u);
        return ResponseEntity.ok(new UserProfileDto(u));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDto> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        User u = currentUser();
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : ".jpg";
        Path target = avatarDir.resolve(u.getId() + ext);
        Files.write(target, file.getBytes());
        u.setAvatarPath(target.toString());
        userRepo.save(u);
        return ResponseEntity.ok(new UserProfileDto(u));
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String userId) throws IOException {
        var opt = userRepo.findById(userId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        Path path;
        if (u.getAvatarPath() != null && !u.getAvatarPath().isBlank()) {
            path = Paths.get(u.getAvatarPath());
        } else {
            // Sin avatar: devolver 204 No Content o un placeholder en el frontend
            return ResponseEntity.noContent().build();
        }
        if (!Files.exists(path)) return ResponseEntity.notFound().build();
        FileSystemResource res = new FileSystemResource(path);
        String ct = Files.probeContentType(path);
        if (ct == null) ct = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ct)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(res);
    }
}