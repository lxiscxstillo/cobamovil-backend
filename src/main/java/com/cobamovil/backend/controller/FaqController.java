package com.cobamovil.backend.controller;

import com.cobamovil.backend.entity.Faq;
import com.cobamovil.backend.repository.FaqRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faq")
public class FaqController {
    private final FaqRepository repo;
    public FaqController(FaqRepository repo) { this.repo = repo; }

    @GetMapping
    public ResponseEntity<List<Faq>> publicList() {
        return ResponseEntity.ok(repo.findByActiveTrueOrderBySortOrderAscIdAsc());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<Faq>> adminList() { return ResponseEntity.ok(repo.findAll()); }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Faq> create(@RequestBody Faq faq) { return ResponseEntity.ok(repo.save(faq)); }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Faq> update(@PathVariable Long id, @RequestBody Faq faq) {
        Faq f = repo.findById(id).orElseThrow();
        f.setQuestion(faq.getQuestion());
        f.setAnswer(faq.getAnswer());
        f.setActive(faq.isActive());
        f.setSortOrder(faq.getSortOrder());
        return ResponseEntity.ok(repo.save(f));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

