package com.freshcut.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.freshcut.db.model.ServiceItem;
import com.freshcut.db.repository.ServiceItemRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/services")
@CrossOrigin(origins = "*")
public class AdminServiceItemController {
    private final ServiceItemRepository repo;

    public AdminServiceItemController(ServiceItemRepository repo) { this.repo = repo; }

    @GetMapping
    public List<ServiceItem> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceItem> getOne(@PathVariable String id) {
        return repo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ServiceItem create(@Valid @RequestBody ServiceItem s) { return repo.save(s); }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceItem> update(@PathVariable String id, @Valid @RequestBody ServiceItem s) {
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