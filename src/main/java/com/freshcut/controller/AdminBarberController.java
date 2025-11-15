package com.freshcut.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.freshcut.db.model.Barber;
import com.freshcut.db.repository.BarberRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/barbers")
@CrossOrigin(origins = "*")
public class AdminBarberController {
    private final BarberRepository repo;

    public AdminBarberController(BarberRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Barber> list() { return repo.findAll(); }

    @PostMapping
    public Barber create(@Valid @RequestBody Barber b) { return repo.save(b); }

    @PutMapping("/{id}")
    public ResponseEntity<Barber> update(@PathVariable String id, @Valid @RequestBody Barber b) {
        return repo.findById(id)
            .map(existing -> {
                b.setId(existing.getId());
                return ResponseEntity.ok(repo.save(b));
            }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}