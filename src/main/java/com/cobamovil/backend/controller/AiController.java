package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.CareWeekPlanResponseDTO;
import com.cobamovil.backend.dto.NormalizeNotesRequestDTO;
import com.cobamovil.backend.dto.NormalizeNotesResponseDTO;
import com.cobamovil.backend.dto.PetAiRecommendationResponse;
import com.cobamovil.backend.dto.PetsOverviewResponseDTO;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.UserRepository;
import com.cobamovil.backend.service.AiRecommendationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiRecommendationService aiRecommendationService;
    private final UserRepository userRepository;
    private final ConcurrentHashMap<String, RateLimit> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    private boolean isRateLimited(String key) {
        RateLimit rl = rateLimitMap.computeIfAbsent(key, k -> new RateLimit());
        long now = System.currentTimeMillis();
        if (now - rl.windowStart > WINDOW_MS) {
            rl.windowStart = now;
            rl.attempts = 0;
        }
        rl.attempts++;
        return rl.attempts > MAX_ATTEMPTS;
    }

    private static class RateLimit {
        long windowStart = System.currentTimeMillis();
        int attempts = 0;
    }

    public AiController(AiRecommendationService aiRecommendationService,
                        UserRepository userRepository) {
        this.aiRecommendationService = aiRecommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pets/{petId}/recommendation")
    public ResponseEntity<PetAiRecommendationResponse> recommendForPet(@PathVariable Long petId,
                                                                       Authentication authentication,
                                                                       HttpServletRequest request) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String key = currentUser.getId() + ":" + request.getRemoteAddr();
        if (isRateLimited(key)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new PetAiRecommendationResponse(
                            petId,
                            null,
                            null,
                            null,
                            "Has solicitado demasiadas recomendaciones en poco tiempo. Intenta de nuevo en unos minutos."
                    ));
        }

        PetAiRecommendationResponse response =
                aiRecommendationService.recommendForPet(petId, currentUser.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/care-week-plan")
    public ResponseEntity<CareWeekPlanResponseDTO> weeklyCarePlan(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        CareWeekPlanResponseDTO dto = aiRecommendationService.generateWeeklyCarePlanForCurrentUser(currentUser);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/pets/overview")
    public ResponseEntity<PetsOverviewResponseDTO> petsOverview(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        PetsOverviewResponseDTO dto = aiRecommendationService.generatePetsOverviewForUser(currentUser);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/pets/normalize-notes")
    public ResponseEntity<NormalizeNotesResponseDTO> normalizePetNotes(
            @RequestBody @jakarta.validation.Valid NormalizeNotesRequestDTO request) {
        NormalizeNotesResponseDTO dto = aiRecommendationService.normalizePetNotes(request);
        return ResponseEntity.ok(dto);
    }
}
