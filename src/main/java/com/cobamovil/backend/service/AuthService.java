package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.AuthResponseDTO;
import com.cobamovil.backend.dto.LoginRequestDTO;
import com.cobamovil.backend.dto.RegisterRequestDTO;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.UserRepository;
import com.cobamovil.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            // Buscar el usuario por username o, si no existe, por email (soporta ambos tipos de login)
            User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado")));

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenProvider.getExpirationInSeconds());

            return new AuthResponseDTO(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                expiresAt
            );

        } catch (Exception e) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    public AuthResponseDTO register(RegisterRequestDTO registerRequest) {
        // Validar duplicados
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("El username '" + registerRequest.getUsername() + "' ya existe");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("El email '" + registerRequest.getEmail() + "' ya existe");
        }

        // Crear usuario
        User user = new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            passwordEncoder.encode(registerRequest.getPassword())
        );
        // Phone is now mandatory (validated at DTO level)
        user.setPhone(registerRequest.getPhone());
        User savedUser = userRepository.save(user);

        // Autenticar con username y password plano para generar token
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                registerRequest.getUsername(),
                registerRequest.getPassword()
            )
        );

        String jwt = tokenProvider.generateToken(authentication);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenProvider.getExpirationInSeconds());

        return new AuthResponseDTO(
            jwt,
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRole(),
            expiresAt
        );
    }
}

