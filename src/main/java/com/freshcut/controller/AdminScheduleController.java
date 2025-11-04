package com.freshcut.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.freshcut.db.model.Schedule;
import com.freshcut.db.repository.ScheduleRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/schedules")
@CrossOrigin(origins = "*")
public class AdminScheduleController {
    private final ScheduleRepository repo;

    public AdminScheduleController(ScheduleRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Schedule> list() { return repo.findAll(); }

    @PostMapping
    public Schedule create(@Valid @RequestBody Schedule s) { return repo.save(s); }

    @PutMapping("/{id}")
    public ResponseEntity<Schedule> update(@PathVariable String id, @Valid @RequestBody Schedule s) {
        return repo.findById(id)
            .map(existing -> {
                s.setId(existing.getId());
                return ResponseEntity.ok(repo.save(s));
            }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}