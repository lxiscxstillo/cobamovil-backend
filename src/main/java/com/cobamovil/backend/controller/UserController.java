package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.UserCreateDTO;
import com.cobamovil.backend.dto.UserResponseDTO;
import com.cobamovil.backend.dto.UserUpdateDTO;
import com.cobamovil.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gestión de usuarios")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    // CREATE - Crear nuevo usuario
    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Usuario ya existe (username o email duplicado)")
    })
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        try {
            UserResponseDTO createdUser = userService.createUser(userCreateDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    // READ - Obtener todos los usuarios con paginación
    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene una lista paginada de usuarios")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    })
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
        @Parameter(description = "Número de página (inicia en 0)") 
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Tamaño de página") 
        @RequestParam(defaultValue = "10") int size,
        
        @Parameter(description = "Campo por el cual ordenar") 
        @RequestParam(defaultValue = "id") String sortBy,
        
        @Parameter(description = "Dirección de ordenamiento (asc/desc)") 
        @RequestParam(defaultValue = "asc") String sortDir,
        
        @Parameter(description = "Buscar por username (opcional)") 
        @RequestParam(required = false) String search
    ) {
        // Crear el objeto Sort
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : 
            Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserResponseDTO> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsersByUsername(search, pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }
        
        return ResponseEntity.ok(users);
    }
    
    // READ - Obtener usuario por ID
    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene un usuario específico por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserResponseDTO> getUserById(
        @Parameter(description = "ID del usuario") @PathVariable Long id
    ) {
        try {
            UserResponseDTO user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // UPDATE - Actualizar usuario
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Conflicto con datos existentes")
    })
    public ResponseEntity<UserResponseDTO> updateUser(
        @Parameter(description = "ID del usuario") @PathVariable Long id,
        @Valid @RequestBody UserUpdateDTO userUpdateDTO
    ) {
        try {
            UserResponseDTO updatedUser = userService.updateUser(id, userUpdateDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }
    }
    
    // DELETE - Eliminar usuario
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "ID del usuario") @PathVariable Long id
    ) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // UTILITY - Verificar si usuario existe
    @GetMapping("/{id}/exists")
    @Operation(summary = "Verificar existencia", description = "Verifica si un usuario existe")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificación completada")
    })
    public ResponseEntity<Boolean> userExists(
        @Parameter(description = "ID del usuario") @PathVariable Long id
    ) {
        boolean exists = userService.userExists(id);
        return ResponseEntity.ok(exists);
    }
}
