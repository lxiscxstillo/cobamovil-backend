package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.UserCreateDTO;
import com.cobamovil.backend.dto.UserResponseDTO;
import com.cobamovil.backend.dto.UserUpdateDTO;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    // Crear usuario
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {
        // Verificar que el username no exista
        if (userRepository.existsByUsername(userCreateDTO.getUsername())) {
            throw new RuntimeException("El username '" + userCreateDTO.getUsername() + "' ya existe");
        }
        
        // Verificar que el email no exista
        if (userRepository.existsByEmail(userCreateDTO.getEmail())) {
            throw new RuntimeException("El email '" + userCreateDTO.getEmail() + "' ya existe");
        }
        
        // Crear el usuario
        User user = new User(
            userCreateDTO.getUsername(),
            userCreateDTO.getEmail(),
            userCreateDTO.getPassword() // TODO: Encriptar password más adelante
        );
        
        User savedUser = userRepository.save(user);
        return convertToResponseDTO(savedUser);
    }
    
    // Obtener usuario por ID
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario con ID " + id + " no encontrado"));
        return convertToResponseDTO(user);
    }
    
    // Obtener todos los usuarios con paginación
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(this::convertToResponseDTO);
    }
    
    // Buscar usuarios por username
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsersByUsername(String search, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(search, pageable)
            .map(this::convertToResponseDTO);
    }
    
    // Actualizar usuario
    public UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario con ID " + id + " no encontrado"));
        
        // Actualizar campos si no son null
        if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().trim().isEmpty()) {
            // Verificar que el nuevo username no exista en otro usuario
            if (userRepository.existsByUsername(userUpdateDTO.getUsername()) && 
                !user.getUsername().equals(userUpdateDTO.getUsername())) {
                throw new RuntimeException("El username '" + userUpdateDTO.getUsername() + "' ya existe");
            }
            user.setUsername(userUpdateDTO.getUsername());
        }
        
        if (userUpdateDTO.getEmail() != null && !userUpdateDTO.getEmail().trim().isEmpty()) {
            // Verificar que el nuevo email no exista en otro usuario
            if (userRepository.existsByEmail(userUpdateDTO.getEmail()) && 
                !user.getEmail().equals(userUpdateDTO.getEmail())) {
                throw new RuntimeException("El email '" + userUpdateDTO.getEmail() + "' ya existe");
            }
            user.setEmail(userUpdateDTO.getEmail());
        }
        
        if (userUpdateDTO.getPassword() != null && !userUpdateDTO.getPassword().trim().isEmpty()) {
            user.setPassword(userUpdateDTO.getPassword()); // TODO: Encriptar password más adelante
        }
        
        User updatedUser = userRepository.save(user);
        return convertToResponseDTO(updatedUser);
    }
    
    // Eliminar usuario
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario con ID " + id + " no encontrado");
        }
        userRepository.deleteById(id);
    }
    
    // Verificar si un usuario existe
    @Transactional(readOnly = true)
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }
    
    // Método auxiliar para convertir Entity a ResponseDTO
    private UserResponseDTO convertToResponseDTO(User user) {
        return new UserResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
