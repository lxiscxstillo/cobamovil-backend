package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.Pet;
import com.cobamovil.backend.entity.PetHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetHistoryRepository extends JpaRepository<PetHistory, Long> {
    List<PetHistory> findByPetOrderByCreatedAtDesc(Pet pet);
}

