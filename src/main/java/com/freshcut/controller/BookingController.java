package com.freshcut.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.freshcut.dto.BookingRequest;
import com.freshcut.db.model.Booking;
import com.freshcut.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Booking> list() {
        return bookingService.list();
    }

    // Añadido: listado de reservas del usuario autenticado
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> my() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getName();
        return ResponseEntity.ok(bookingService.listByClientName(email));
    }

    @PostMapping
    public ResponseEntity<Booking> create(@Valid @RequestBody BookingRequest req) {
        Booking saved = bookingService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> get(@PathVariable String id) {
        Optional<Booking> b = bookingService.findOne(id);
        return b.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable String id, @Valid @RequestBody BookingRequest req) {
        Booking updated = bookingService.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancel(@PathVariable String id) {
        Booking cancelled = bookingService.cancel(id);
        return ResponseEntity.ok(cancelled);
    }

    // NUEVO: completar una reserva
    @PostMapping("/{id}/complete")
    public ResponseEntity<Booking> complete(@PathVariable String id) {
        Booking completed = bookingService.complete(id);
        return ResponseEntity.ok(completed);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}