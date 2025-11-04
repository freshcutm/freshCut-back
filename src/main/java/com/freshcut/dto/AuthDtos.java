package com.freshcut.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public static class RegisterRequest {
        private String name; // opcional: nombre, usado para crear perfil BARBER
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        private String role; // optional: ADMIN/USER/BARBER
        private String barberId; // opcional si role=BARBER: id del barbero asociado

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getBarberId() { return barberId; }
        public void setBarberId(String barberId) { this.barberId = barberId; }
    }

    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private String email;
        private String role;

        public AuthResponse() {}
        public AuthResponse(String token, String email, String role) {
            this.token = token; this.email = email; this.role = role;
        }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class ForgotRequest {
        @NotBlank @Email
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String code;
        @NotBlank
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}