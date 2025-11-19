package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.GroomerCreateDTO;
import com.cobamovil.backend.dto.GroomerProfileDTO;
import com.cobamovil.backend.entity.CutRecord;
import com.cobamovil.backend.service.GroomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groomers")
public class GroomerController {
    private final GroomerService groomerService;
    public GroomerController(GroomerService groomerService) { this.groomerService = groomerService; }

    // Public list of groomers (for selection when booking)
    @GetMapping
    public ResponseEntity<List<GroomerProfileDTO>> list() { return ResponseEntity.ok(groomerService.listGroomers()); }

    @GetMapping("/{userId}")
    public ResponseEntity<GroomerProfileDTO> profile(@PathVariable Long userId) { return ResponseEntity.ok(groomerService.getProfile(userId)); }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<CutRecord>> history(@PathVariable Long userId) { return ResponseEntity.ok(groomerService.getHistory(userId)); }

    // Admin creates/upserts profiles and users with role GROOMER
    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<GroomerProfileDTO> create(@Valid @RequestBody GroomerCreateDTO dto) {
        return ResponseEntity.ok(groomerService.createGroomer(dto));
    }

    @PutMapping("/{userId}/admin")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<GroomerProfileDTO> update(@PathVariable Long userId, @RequestBody GroomerProfileDTO dto) {
        dto.setUserId(userId);
        return ResponseEntity.ok(groomerService.updateProfile(userId, dto));
    }

    @DeleteMapping("/{userId}/admin")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        groomerService.deleteGroomer(userId);
        return ResponseEntity.noContent().build();
    }
}
