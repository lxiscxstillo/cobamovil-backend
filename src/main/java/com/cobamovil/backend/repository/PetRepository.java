package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.Pet;
import com.cobamovil.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByOwner(User owner);
}

