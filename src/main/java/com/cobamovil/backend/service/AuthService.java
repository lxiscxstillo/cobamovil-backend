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
// ...existing code...

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
            
            User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(1); // Token válido por 1 día
            
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
        // Verificar que el username no exista
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("El username '" + registerRequest.getUsername() + "' ya existe");
        }
        
        // Verificar que el email no exista
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("El email '" + registerRequest.getEmail() + "' ya existe");
        }
        
        // Crear el usuario con password encriptado
        User user = new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            passwordEncoder.encode(registerRequest.getPassword())
        );
        
        User savedUser = userRepository.save(user);
        
        // Generar token para el nuevo usuario
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            savedUser.getUsername(),
            savedUser.getPassword()
        );
        
        String jwt = tokenProvider.generateToken(authentication);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        
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
