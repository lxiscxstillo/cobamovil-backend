package com.cobamovil.backend.config;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Manejo de errores de validación
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.warn("Error de validación en {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Datos inválidos");
        // userMessage: mensaje amigable para el usuario final
        response.put("message", "Algunos datos no son válidos. Revisa los campos marcados e inténtalo de nuevo.");
        response.put("validationErrors", errors);
        response.put("path", request.getDescription(false));

        return ResponseEntity.badRequest().body(response);
    }

    // Manejo de errores de autenticación
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex, WebRequest request) {
        logger.warn("Error de autenticación en {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "No autorizado");
        // userMessage: explica que faltan credenciales válidas
        response.put("message", "No pudimos iniciar sesión con esos datos. Revisa tu usuario y contraseña e inténtalo de nuevo.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // Manejo de errores de autorización
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Acceso denegado en {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Acceso denegado");
        // userMessage: amable y claro para el usuario
        response.put("message", "No tienes permisos para realizar esta acción. Si crees que es un error, contacta al administrador.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Errores de recurso no encontrado (ej. reserva inexistente)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        logger.warn("Recurso no encontrado en {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "No encontrado");
        // userMessage: mensaje claro cuando la reserva ya no existe
        response.put("message", "No encontramos esta reserva. Es posible que ya haya sido modificada o eliminada.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Manejo de reglas de negocio (ej. cancelar o reprogramar en estado no permitido)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        logger.warn("Operación no permitida en {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Operación no permitida");
        // userMessage: usamos el mensaje de negocio ya preparado en BookingService
        response.put("message", ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Esta operación no puede realizarse en el estado actual de la reserva.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Manejo de errores generales de runtime
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Error de runtime en {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Solicitud incorrecta");
        // userMessage: no exponemos el mensaje técnico; damos una explicación genérica
        response.put("message", "La operación no pudo completarse con los datos enviados. Revisa la información e inténtalo de nuevo.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.badRequest().body(response);
    }

    // Manejo de errores generales
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Error interno en {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Error interno");
        // userMessage: mensaje genérico y no técnico
        response.put("message", "Ha ocurrido un error inesperado en el sistema. Inténtalo de nuevo en unos minutos.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(Exception ex, WebRequest request) {
        logger.warn("JSON malformado en {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Contenido inválido");
        // userMessage: explica que el formato enviado no es correcto
        response.put("message", "La información enviada tiene un formato incorrecto. Revisa los datos e inténtalo de nuevo.");
        response.put("path", request.getDescription(false));

        return ResponseEntity.badRequest().body(response);
    }
}

