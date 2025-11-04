package com.freshcut.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.freshcut.db.model.Barber;
import com.freshcut.db.model.Schedule;
import com.freshcut.db.model.User;
import com.freshcut.db.repository.BarberRepository;
import com.freshcut.db.repository.BookingRepository;
import com.freshcut.db.repository.ScheduleRepository;
import com.freshcut.db.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/barber")
@CrossOrigin(origins = "*")
public class BarberController {
    private final UserRepository userRepo;
    private final BarberRepository barberRepo;
    private final BookingRepository bookingRepo;
    private final ScheduleRepository scheduleRepo;

    public BarberController(UserRepository userRepo, BarberRepository barberRepo, BookingRepository bookingRepo, ScheduleRepository scheduleRepo) {
        this.userRepo = userRepo;
        this.barberRepo = barberRepo;
        this.bookingRepo = bookingRepo;
        this.scheduleRepo = scheduleRepo;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? (String) auth.getPrincipal() : null;
        if (email == null) throw new IllegalStateException("No autenticado");
        return userRepo.findByEmail(email).orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    @GetMapping("/me")
    public ResponseEntity<Barber> me() {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        return barberRepo.findById(u.getBarberId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<Barber> updateMe(@Valid @RequestBody Barber payload) {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        return barberRepo.findById(u.getBarberId()).map(existing -> {
            // Permitir actualizar nombre, especialidades, activo y nuevos campos
            existing.setName(payload.getName() != null ? payload.getName() : existing.getName());
            existing.setSpecialties(payload.getSpecialties() != null ? payload.getSpecialties() : existing.getSpecialties());
            existing.setBio(payload.getBio() != null ? payload.getBio() : existing.getBio());
            existing.setExperienceYears(payload.getExperienceYears() != null ? payload.getExperienceYears() : existing.getExperienceYears());
            existing.setCutTypes(payload.getCutTypes() != null ? payload.getCutTypes() : existing.getCutTypes());
            existing.setActive(payload.isActive());
            return ResponseEntity.ok(barberRepo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> myBookings() {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        Barber b = barberRepo.findById(u.getBarberId()).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(bookingRepo.findByBarber(b.getName()));
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<Schedule>> mySchedules() {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(scheduleRepo.findByBarberId(u.getBarberId()));
    }

    @PostMapping("/schedules")
    public ResponseEntity<Schedule> createSchedule(@Valid @RequestBody Schedule s) {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        s.setBarberId(u.getBarberId());
        Schedule saved = scheduleRepo.save(s);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable String id, @Valid @RequestBody Schedule s) {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        return scheduleRepo.findById(id).map(existing -> {
            if (!u.getBarberId().equals(existing.getBarberId())) return ResponseEntity.status(403).body((Schedule) null);
            existing.setDayOfWeek(s.getDayOfWeek());
            existing.setStartTime(s.getStartTime());
            existing.setEndTime(s.getEndTime());
            return ResponseEntity.ok(scheduleRepo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String id) {
        User u = currentUser();
        if (u.getBarberId() == null || u.getBarberId().isBlank()) return ResponseEntity.notFound().build();
        var opt = scheduleRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var existing = opt.get();
        if (!u.getBarberId().equals(existing.getBarberId())) return ResponseEntity.status(403).build();
        scheduleRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}