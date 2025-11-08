package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.CutRecord;
import com.cobamovil.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CutRecordRepository extends JpaRepository<CutRecord, Long> {
    List<CutRecord> findByGroomerOrderByDateDescTimeDesc(User groomer);
    List<CutRecord> findByGroomerAndDate(User groomer, LocalDate date);
}

