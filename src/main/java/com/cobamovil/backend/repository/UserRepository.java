package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Buscar por username
    Optional<User> findByUsername(String username);
    
    // Buscar por email
    Optional<User> findByEmail(String email);
    
    // Verificar si existe por username
    boolean existsByUsername(String username);
    
    // Verificar si existe por email
    boolean existsByEmail(String email);
    
    // Buscar usuarios por username que contenga el texto (case insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> findByUsernameContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    
    // Buscar usuarios por email que contenga el texto (case insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> findByEmailContainingIgnoreCase(@Param("search") String search, Pageable pageable);

    // Find users by role
    @Query("SELECT u FROM User u WHERE u.role = :role")
    java.util.List<User> findByRole(@Param("role") String role);
}
