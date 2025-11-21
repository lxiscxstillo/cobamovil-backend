package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.PetAiRecommendationResponse;
import com.cobamovil.backend.entity.Booking;
import com.cobamovil.backend.entity.Pet;
import com.cobamovil.backend.entity.ServiceType;
import com.cobamovil.backend.repository.BookingRepository;
import com.cobamovil.backend.repository.PetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AiRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(AiRecommendationService.class);
    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final PetRepository petRepository;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiRecommendationService(PetRepository petRepository,
                                   BookingRepository bookingRepository,
                                   ObjectMapper objectMapper) {
        this.petRepository = petRepository;
        this.bookingRepository = bookingRepository;
        this.objectMapper = objectMapper;
    }

    public PetAiRecommendationResponse recommendForPet(Long petId, Long currentUserId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new EntityNotFoundException("Pet not found"));

        if (pet.getOwner() == null || pet.getOwner().getId() == null ||
                !pet.getOwner().getId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this pet");
        }

        Optional<Booking> lastBookingOpt = bookingRepository.findTopByPetOrderByDateDescTimeDesc(pet);
        Booking lastBooking = lastBookingOpt.orElse(null);

        String contextText = buildContext(pet, lastBooking);
        return generateRecommendationWithGeminiOrFallback(pet, lastBooking, contextText);
    }

    private String buildContext(Pet pet, Booking lastBooking) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mascota: ").append(nullSafe(pet.getName())).append(". ");
        sb.append("Raza: ").append(nullSafe(pet.getBreed())).append(". ");
        if (pet.getAge() != null) {
            sb.append("Edad aproximada: ").append(pet.getAge()).append(" años. ");
        }
        if (pet.getWeight() != null) {
            sb.append("Peso aproximado: ").append(pet.getWeight()).append(" kg. ");
        }
        if (pet.getSex() != null) {
            sb.append("Sexo: ").append("M".equalsIgnoreCase(pet.getSex()) ? "Macho" : "Hembra").append(". ");
        }
        if (pet.getBehavior() != null && !pet.getBehavior().isBlank()) {
            sb.append("Comportamiento: ").append(pet.getBehavior()).append(". ");
        }
        if (pet.getHealthNotes() != null && !pet.getHealthNotes().isBlank()) {
            sb.append("Notas de salud: ").append(pet.getHealthNotes()).append(". ");
        }
        if (pet.getMedicalConditions() != null && !pet.getMedicalConditions().isBlank()) {
            sb.append("Condiciones médicas crónicas: ").append(pet.getMedicalConditions()).append(". ");
        }
        if (pet.getVaccinations() != null && !pet.getVaccinations().isBlank()) {
            sb.append("Registro de vacunas: ").append(pet.getVaccinations()).append(". ");
        }
        if (pet.getDeworming() != null && !pet.getDeworming().isBlank()) {
            sb.append("Desparasitaciones registradas: ").append(pet.getDeworming()).append(". ");
        }
        if (pet.getLastGroomDate() != null) {
            sb.append("Fecha del último servicio de peluquería registrada en la ficha: ")
                    .append(pet.getLastGroomDate()).append(". ");
        }

        if (lastBooking != null) {
            LocalDate date = lastBooking.getDate();
            LocalTime time = lastBooking.getTime();
            ServiceType serviceType = lastBooking.getServiceType();
            sb.append("Última reserva de grooming registrada en el sistema: ");
            if (serviceType != null) {
                sb.append("tipo de servicio ").append(serviceType.name()).append(", ");
            }
            if (date != null) {
                sb.append("en fecha ").append(date).append(" ");
            }
            if (time != null) {
                sb.append("a las ").append(time).append(" ");
            }
            sb.append(". ");
        }

        sb.append("Responde en español.");

        return sb.toString();
    }

    private PetAiRecommendationResponse generateRecommendationWithGeminiOrFallback(Pet pet,
                                                                                  Booking lastBooking,
                                                                                  String contextText) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            logger.info("GEMINI_API_KEY not configured, using local fallback recommendation.");
            return generateFallbackRecommendation(pet, lastBooking);
        }

        try {
            String prompt = "INSTRUCCIONES: Eres un experto en grooming canino y felino." +
                    " Con el siguiente contexto de la mascota, genera una recomendación en formato JSON " +
                    "con las claves 'recommendedServiceType', 'recommendedFrequency' y 'advice'. " +
                    "El campo 'advice' debe ser un texto claro en español dirigido al dueño, " +
                    "sin rodeos técnicos. Usa valores como BATH, HAIRCUT, NAIL_TRIM o FULL_GROOMING " +
                    "para 'recommendedServiceType'. CONTEXTO: " + contextText;

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(textPart));

            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String rawResponse = restTemplate.postForObject(GEMINI_ENDPOINT, requestEntity, String.class);
            if (rawResponse == null || rawResponse.isBlank()) {
                logger.warn("Empty response from Gemini, using fallback recommendation.");
                return generateFallbackRecommendation(pet, lastBooking);
            }

            String aiText = extractTextFromGeminiResponse(rawResponse);
            if (aiText == null || aiText.isBlank()) {
                logger.warn("Could not extract text from Gemini response, using fallback recommendation.");
                return generateFallbackRecommendation(pet, lastBooking);
            }

            PetAiRecommendationResponse parsed = parseAiJson(aiText, pet);
            if (parsed != null && parsed.getAdvice() != null && !parsed.getAdvice().isBlank()) {
                return parsed;
            }

            // If parsing failed but we have plain text, use it as advice only.
            PetAiRecommendationResponse response = new PetAiRecommendationResponse();
            response.setPetId(pet.getId());
            response.setPetName(pet.getName());
            response.setAdvice(aiText.trim());
            response.setRecommendedServiceType(null);
            response.setRecommendedFrequency(null);
            return response;

        } catch (Exception ex) {
            logger.warn("Error while calling Gemini API, falling back to local recommendation: {}", ex.getMessage());
            return generateFallbackRecommendation(pet, lastBooking);
        }
    }

    private String extractTextFromGeminiResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return null;
            }
            JsonNode candidate0 = candidates.get(0);
            JsonNode content = candidate0.path("content");
            JsonNode parts = content.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return null;
            }
            JsonNode part0 = parts.get(0);
            JsonNode textNode = part0.path("text");
            if (textNode.isMissingNode()) {
                return null;
            }
            return textNode.asText();
        } catch (Exception e) {
            logger.warn("Failed to parse Gemini response JSON: {}", e.getMessage());
            return null;
        }
    }

    private PetAiRecommendationResponse parseAiJson(String aiText, Pet pet) {
        try {
            JsonNode root = objectMapper.readTree(aiText);
            if (!root.isObject()) {
                return null;
            }
            String recommendedServiceType = textOrNull(root, "recommendedServiceType");
            String recommendedFrequency = textOrNull(root, "recommendedFrequency");
            String advice = textOrNull(root, "advice");

            if (advice == null || advice.isBlank()) {
                return null;
            }

            PetAiRecommendationResponse response = new PetAiRecommendationResponse();
            response.setPetId(pet.getId());
            response.setPetName(pet.getName());
            response.setRecommendedServiceType(emptyToNull(recommendedServiceType));
            response.setRecommendedFrequency(emptyToNull(recommendedFrequency));
            response.setAdvice(advice.trim());
            return response;
        } catch (Exception e) {
            logger.warn("AI JSON payload could not be parsed as object: {}", e.getMessage());
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode f = node.get(field);
        if (f == null || f.isNull()) {
            return null;
        }
        String v = f.asText(null);
        return (v == null || v.isBlank()) ? null : v;
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private PetAiRecommendationResponse generateFallbackRecommendation(Pet pet, Booking lastBooking) {
        String recommendedServiceType = "FULL_GROOMING";
        String recommendedFrequency = "Cada 8 semanas";

        boolean hasSkinIssues = containsKeyword(pet.getHealthNotes(), "piel", "dermat", "alergia")
                || containsKeyword(pet.getMedicalConditions(), "piel", "dermat", "alergia");

        if (hasSkinIssues) {
            recommendedServiceType = "BATH";
            recommendedFrequency = "Cada 3–4 semanas";
        } else if (pet.getAge() != null && pet.getAge() < 7) {
            // Regla simple para razas pequeñas/medias jóvenes: si el peso es bajo o no está informado
            if (pet.getWeight() == null || pet.getWeight() <= 15.0) {
                recommendedServiceType = "FULL_GROOMING";
                recommendedFrequency = "Cada 6–8 semanas";
            }
        }

        StringBuilder advice = new StringBuilder();
        advice.append("Según la información registrada de ")
                .append(nullSafe(pet.getName()))
                .append(", se recomienda un servicio de ")
                .append(readableService(recommendedServiceType))
                .append(" con una frecuencia de ")
                .append(recommendedFrequency)
                .append(" para mantener su higiene y bienestar.");

        if (lastBooking != null && lastBooking.getDate() != null) {
            advice.append(" La última cita registrada fue el ")
                    .append(formatDateTime(lastBooking.getDate(), lastBooking.getTime()))
                    .append(", lo que también se tiene en cuenta para esta sugerencia.");
        } else if (pet.getLastGroomDate() != null) {
            advice.append(" También se considera la fecha de último servicio registrada en la ficha (")
                    .append(pet.getLastGroomDate())
                    .append(").");
        }

        advice.append(" Recuerda complementar con cepillado en casa y consultar a tu veterinario ante cualquier cambio de salud.");

        PetAiRecommendationResponse response = new PetAiRecommendationResponse();
        response.setPetId(pet.getId());
        response.setPetName(pet.getName());
        response.setRecommendedServiceType(recommendedServiceType);
        response.setRecommendedFrequency(recommendedFrequency);
        response.setAdvice(advice.toString());
        return response;
    }

    private boolean containsKeyword(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String k : keywords) {
            if (lower.contains(k.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String readableService(String recommendedServiceType) {
        if (recommendedServiceType == null) {
            return "grooming";
        }
        try {
            ServiceType type = ServiceType.valueOf(recommendedServiceType);
            return switch (type) {
                case BATH -> "baño";
                case HAIRCUT -> "corte de pelo";
                case NAIL_TRIM -> "corte de uñas";
                case FULL_GROOMING -> "grooming completo";
            };
        } catch (IllegalArgumentException ex) {
            return "grooming";
        }
    }

    private String formatDateTime(LocalDate date, LocalTime time) {
        try {
            if (date == null) {
                return "";
            }
            LocalDateTime dt = time != null ? LocalDateTime.of(date, time) : date.atStartOfDay();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
            return dt.format(formatter);
        } catch (Exception e) {
            return date.toString();
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
