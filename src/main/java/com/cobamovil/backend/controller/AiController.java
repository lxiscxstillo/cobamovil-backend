package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.PetAiRecommendationResponse;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.UserRepository;
import com.cobamovil.backend.service.AiRecommendationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiRecommendationService aiRecommendationService;
    private final UserRepository userRepository;

    public AiController(AiRecommendationService aiRecommendationService,
                        UserRepository userRepository) {
        this.aiRecommendationService = aiRecommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pets/{petId}/recommendation")
    public ResponseEntity<PetAiRecommendationResponse> recommendForPet(@PathVariable Long petId,
                                                                       Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PetAiRecommendationResponse response =
                aiRecommendationService.recommendForPet(petId, currentUser.getId());

        return ResponseEntity.ok(response);
    }
}

