package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.AuthResponseDTO;
import com.cobamovil.backend.dto.LoginRequestDTO;
import com.cobamovil.backend.dto.RegisterRequestDTO;
import com.cobamovil.backend.service.AuthService;
import com.cobamovil.backend.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticación")
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final ConcurrentHashMap<String, RateLimit> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    private boolean isRateLimited(String ip) {
        RateLimit rl = rateLimitMap.computeIfAbsent(ip, k -> new RateLimit());
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
    
    private final AuthService authService;
    
    @Autowired
    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve un token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login exitoso"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (isRateLimited(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intenta de nuevo en un minuto.");
        }

        AuthResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Usuario ya existe")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (isRateLimited(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos. Intenta de nuevo en un minuto.");
        }

        AuthResponseDTO response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida el token del usuario (logout real)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout exitoso"),
        @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no proporcionado");
        }
        String token = bearerToken.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido o expirado");
        }
        jwtTokenProvider.revokeToken(token);
        return ResponseEntity.ok("Logout exitoso: token revocado");
    }
}

