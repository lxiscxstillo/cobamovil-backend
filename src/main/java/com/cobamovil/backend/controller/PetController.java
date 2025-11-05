package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.PetDTO;
import com.cobamovil.backend.service.PetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public ResponseEntity<List<PetDTO>> list(Authentication auth) {
        return ResponseEntity.ok(petService.myPets(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<PetDTO> create(Authentication auth, @Valid @RequestBody PetDTO dto) {
        return ResponseEntity.ok(petService.create(auth.getName(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetDTO> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody PetDTO dto) {
        return ResponseEntity.ok(petService.update(auth.getName(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        petService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}

