package com.freshcut.db.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String email;
    private String passwordHash;
    private Role role = Role.USER;
private String barberId; // opcional: si el usuario es BARBER
private String name;
private String avatarPath;
private Instant createdAt = Instant.now();
    // Recuperación de contraseña
    private String resetCode; // código temporal enviado por email
    private Instant resetExpiry; // expiración del código

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getBarberId() { return barberId; }
public void setBarberId(String barberId) { this.barberId = barberId; }
public String getName() { return name; }
public void setName(String name) { this.name = name; }
public String getAvatarPath() { return avatarPath; }
public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
public Instant getCreatedAt() { return createdAt; }
public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }
    public Instant getResetExpiry() { return resetExpiry; }
    public void setResetExpiry(Instant resetExpiry) { this.resetExpiry = resetExpiry; }
}