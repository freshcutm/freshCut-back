package com.freshcut.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (request.getCookies() != null) {
            // Fallback: leer JWT desde cookie HttpOnly
            var cookies = request.getCookies();
            for (var c : cookies) {
                if ("AUTH_TOKEN".equals(c.getName())) { token = c.getValue(); break; }
            }
        }
        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtService.parse(token);
                String email = claims.getSubject();
                String role = (String) claims.get("role");
                var auth = new UsernamePasswordAuthenticationToken(email, null,
                        role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}