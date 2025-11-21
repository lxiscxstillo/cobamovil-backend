package com.cobamovil.backend.repository;

import com.cobamovil.backend.entity.Booking;
import com.cobamovil.backend.entity.BookingStatus;
import com.cobamovil.backend.entity.Pet;
import com.cobamovil.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerOrderByDateAscTimeAsc(User customer);
    List<Booking> findByDateAndStatus(LocalDate date, BookingStatus status);
    List<Booking> findByDate(LocalDate date);

    Optional<Booking> findTopByPetOrderByDateDescTimeDesc(Pet pet);
}

