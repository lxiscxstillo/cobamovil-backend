package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.RoutePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    Optional<RoutePlan> findByDate(LocalDate date);
}

