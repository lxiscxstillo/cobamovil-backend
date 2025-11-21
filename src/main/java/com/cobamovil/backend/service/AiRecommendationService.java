package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.CareWeekPlanResponseDTO;
import com.cobamovil.backend.dto.NormalizeNotesRequestDTO;
import com.cobamovil.backend.dto.NormalizeNotesResponseDTO;
import com.cobamovil.backend.dto.PetAiRecommendationResponse;
import com.cobamovil.backend.dto.PetsOverviewResponseDTO;
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

    public CareWeekPlanResponseDTO generateWeeklyCarePlanForCurrentUser(com.cobamovil.backend.entity.User currentUser) {
        List<Pet> pets = petRepository.findByOwner(currentUser);
        if (pets == null || pets.isEmpty()) {
            String text = "Actualmente no tienes mascotas registradas. " +
                    "Registra al menos una mascota para generar un plan de cuidado semanal personalizado.";
            return new CareWeekPlanResponseDTO(text, java.time.OffsetDateTime.now().toString());
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("Usuario: ").append(currentUser.getUsername()).append(". ");
        ctx.append("Mascotas registradas: ").append(pets.size()).append(". ");
        ctx.append("Detalle de mascotas: ");
        for (Pet p : pets) {
            ctx.append("\n- Nombre: ").append(nullSafe(p.getName()));
            if (p.getBreed() != null) ctx.append(", raza: ").append(p.getBreed());
            if (p.getAge() != null) ctx.append(", edad: ").append(p.getAge()).append(" a\u00f1os");
            if (p.getWeight() != null) ctx.append(", peso: ").append(p.getWeight()).append(" kg");
            if (p.getHealthNotes() != null && !p.getHealthNotes().isBlank()) {
                ctx.append(", salud: ").append(p.getHealthNotes());
            }
            if (p.getBehavior() != null && !p.getBehavior().isBlank()) {
                ctx.append(", comportamiento: ").append(p.getBehavior());
            }
            if (p.getLastGroomDate() != null) {
                ctx.append(", \u00faltimo grooming registrado: ").append(p.getLastGroomDate());
            }
            Optional<Booking> last = bookingRepository.findTopByPetOrderByDateDescTimeDesc(p);
            if (last.isPresent()) {
                Booking b = last.get();
                ctx.append(". \u00daltimo servicio en el sistema: ")
                        .append(b.getServiceType() != null ? b.getServiceType().name() : "DESCONOCIDO")
                        .append(" en fecha ").append(b.getDate());
            }
        }

        String prompt = "INSTRUCCIONES: Eres un asistente experto en cuidado de mascotas " +
                "para una peluquer\u00eda canina a domicilio. Con el siguiente contexto de las mascotas del usuario, " +
                "genera un plan semanal de cuidado en espa\u00f1ol, amigable y f\u00e1cil de seguir. " +
                "No des consejos m\u00e9dicos ni diagn\u00f3sticos; solo rutinas de cuidado b\u00e1sico como cepillado, " +
                "revisi\u00f3n visual, limpieza suave, juegos tranquilos y recomendaciones generales. " +
                "Organiza el plan en pasos o vi\u00f1etas por d\u00eda de la semana (Lunes a Domingo) " +
                "y adapta la sugerencia a la edad, peso y notas de salud/comportamiento. " +
                "Devuelve \u00fanicamente el texto del plan semanal, en un formato legible para el usuario final. " +
                "\n\nCONTEXTO:\n" + ctx;

        String text = callGeminiForPlainText(prompt);
        if (text == null || text.isBlank()) {
            text = buildWeeklyPlanFallback(pets);
        }
        return new CareWeekPlanResponseDTO(text, java.time.OffsetDateTime.now().toString());
    }

    public PetsOverviewResponseDTO generatePetsOverviewForUser(com.cobamovil.backend.entity.User currentUser) {
        List<Pet> pets = petRepository.findByOwner(currentUser);
        int total = pets == null ? 0 : pets.size();
        if (total == 0) {
            String text = "Actualmente no tienes mascotas registradas en el sistema. " +
                    "Cuando registres a tus compa\u00f1eros peludos podr\u00e1s ver aqu\u00ed un resumen general " +
                    "de su rutina de cuidado y grooming.";
            return new PetsOverviewResponseDTO(text, 0);
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("Usuario: ").append(currentUser.getUsername())
                .append(". N\u00famero de mascotas: ").append(total).append(". ");
        ctx.append("Mascotas:\n");
        int totalBookings = 0;
        for (Pet p : pets) {
            ctx.append("- ").append(nullSafe(p.getName()));
            if (p.getBreed() != null) ctx.append(" (").append(p.getBreed()).append(")");
            if (p.getAge() != null) ctx.append(", edad ").append(p.getAge()).append(" a\u00f1os");
            if (p.getMedicalConditions() != null && !p.getMedicalConditions().isBlank()) {
                ctx.append(", condiciones m\u00e9dicas: ").append(p.getMedicalConditions());
            }
            if (p.getBehavior() != null && !p.getBehavior().isBlank()) {
                ctx.append(", comportamiento: ").append(p.getBehavior());
            }
            Optional<Booking> last = bookingRepository.findTopByPetOrderByDateDescTimeDesc(p);
            if (last.isPresent()) {
                Booking b = last.get();
                totalBookings++;
                ctx.append(". \u00daltimo servicio: ").append(b.getServiceType() != null ? b.getServiceType().name() : "DESCONOCIDO")
                        .append(" el ").append(b.getDate());
            }
            ctx.append("\n");
        }

        String prompt = "INSTRUCCIONES: Eres un asistente de una peluquer\u00eda canina a domicilio. " +
                "Con el siguiente contexto de las mascotas del usuario, genera un resumen global de su 'familia peluda'. " +
                "Indica cu\u00e1ntas mascotas tiene, c\u00f3mo parece su rutina de grooming en general (por ejemplo, " +
                "si las atiende con frecuencia o si podr\u00eda aumentar la regularidad) y ofrece recomendaciones " +
                "generales de cuidado (no m\u00e9dicas). Responde siempre en espa\u00f1ol, en 1 o 2 p\u00e1rrafos " +
                "con tono cercano y positivo. No inventes datos que no aparezcan en el contexto.\n\nCONTEXTO:\n" + ctx;

        String text = callGeminiForPlainText(prompt);
        if (text == null || text.isBlank()) {
            text = buildPetsOverviewFallback(pets, totalBookings);
        }
        return new PetsOverviewResponseDTO(text, total);
    }

    public NormalizeNotesResponseDTO normalizePetNotes(NormalizeNotesRequestDTO request) {
        String raw = request.getRawText();
        if (raw == null || raw.trim().isEmpty()) {
            return new NormalizeNotesResponseDTO("No hay suficiente texto para mejorar.");
        }
        String ctx = request.getContext() == null ? "" : request.getContext().trim().toUpperCase(Locale.ROOT);
        String contextoLegible = "este texto describe informaci\u00f3n de la mascota";
        if ("SALUD".equals(ctx)) {
            contextoLegible = "este texto describe la salud y cuidados especiales de la mascota";
        } else if ("COMPORTAMIENTO".equals(ctx)) {
            contextoLegible = "este texto describe el comportamiento y personalidad de la mascota";
        }

        String prompt = "INSTRUCCIONES: Eres un asistente de redacci\u00f3n para una ficha de mascota " +
                "en una peluquer\u00eda canina a domicilio. Te doy un texto breve que el usuario escribi\u00f3 sobre su mascota; " +
                contextoLegible + ". Reescribe el texto de forma m\u00e1s clara, con buena ortograf\u00eda y puntuaci\u00f3n, " +
                "en no m\u00e1s de 2 o 3 frases. No inventes informaci\u00f3n nueva ni cambies el significado, " +
                "solo mejora la redacci\u00f3n. Responde solo con el texto mejorado en espa\u00f1ol.\n\nTexto original:\n" + raw;

        String improved = callGeminiForPlainText(prompt);
        if (improved == null || improved.isBlank()) {
            // Fallback sencillo: limpiar espacios y capitalizar primera letra
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                improved = "No hay suficiente texto para mejorar.";
            } else {
                char first = Character.toUpperCase(trimmed.charAt(0));
                improved = first + trimmed.substring(1);
            }
        }
        return new NormalizeNotesResponseDTO(improved);
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

    private String callGeminiForPlainText(String prompt) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            logger.info("GEMINI_API_KEY not configured, skipping Gemini call and returning null.");
            return null;
        }
        try {
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
                logger.warn("Empty response from Gemini for plain text request.");
                return null;
            }
            return extractTextFromGeminiResponse(rawResponse);
        } catch (Exception ex) {
            logger.warn("Error calling Gemini for plain text: {}", ex.getMessage());
            return null;
        }
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

    private String buildWeeklyPlanFallback(List<Pet> pets) {
        int total = pets == null ? 0 : pets.size();
        if (total == 0) {
            return "Actualmente no tienes mascotas registradas. Registra al menos una mascota para recibir un plan de cuidado semanal personalizado.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Esta semana tienes ").append(total).append(" mascota");
        if (total > 1) sb.append("s");
        sb.append(" registrada");
        if (total > 1) sb.append("s");
        sb.append(" en Coba M\u00f3vil. ");
        sb.append("Te sugerimos dedicar al menos unos minutos cada d\u00eda para su cuidado:\n\n");
        sb.append("- Lunes: cepilla suavemente a tus mascotas para reducir el pelo suelto y revisar su piel.\n");
        sb.append("- Mi\u00e9rcoles: revisa ojos, orejas y almohadillas, limpiando solo de forma superficial si lo necesitan.\n");
        sb.append("- Viernes: vuelve a cepillar y revisa si su pelaje requiere un nuevo servicio de grooming.\n");
        sb.append("- Fin de semana: dedica tiempo de juego tranquilo y observa si hay alg\u00fan cambio en su comportamiento o piel.\n\n");
        sb.append("Si notas que el pelaje se enreda r\u00e1pido o que los intervalos entre servicios son muy largos, ");
        sb.append("considera programar una cita de ba\u00f1o o grooming completo para mantener su comodidad y bienestar.");
        return sb.toString();
    }

    private String buildPetsOverviewFallback(List<Pet> pets, int totalBookings) {
        int total = pets == null ? 0 : pets.size();
        if (total == 0) {
            return "Actualmente no tienes mascotas registradas en el sistema. Cuando registres a tus compa\u00f1eros peludos, aqu\u00ed ver\u00e1s un resumen de su rutina de cuidado.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Tienes ").append(total).append(" mascota");
        if (total > 1) sb.append("s");
        sb.append(" registrada");
        if (total > 1) sb.append("s");
        sb.append(" en Coba M\u00f3vil. ");
        if (totalBookings > 0) {
            sb.append("Ya has utilizado nuestros servicios de grooming en varias ocasiones, lo que muestra que te preocupas por su bienestar. ");
        }
        sb.append("Mantener una rutina regular de ba\u00f1o, cepillado y revisi\u00f3n visual te ayudar\u00e1 a detectar a tiempo cualquier cambio ");
        sb.append("en su piel, pelaje o comportamiento.\n\n");
        sb.append("En general, un intervalo de 4 a 8 semanas entre servicios de grooming suele funcionar bien para la mayor\u00eda de mascotas, ");
        sb.append("aunque puede ajustarse seg\u00fan la raza, el tipo de pelaje y el estilo de vida. ");
        sb.append("Si tienes dudas, puedes usar el m\u00f3dulo de reserva para encontrar el tipo de servicio que mejor se adapta a cada una.");
        return sb.toString();
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
