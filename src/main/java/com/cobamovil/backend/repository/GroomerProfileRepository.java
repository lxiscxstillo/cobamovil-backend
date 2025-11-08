package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.GroomerProfile;
import com.cobamovil.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroomerProfileRepository extends JpaRepository<GroomerProfile, Long> {
    Optional<GroomerProfile> findByUser(User user);
}

