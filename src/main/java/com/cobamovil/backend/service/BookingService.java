package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.BookingCreateDTO;
import com.cobamovil.backend.dto.BookingResponseDTO;
import com.cobamovil.backend.entity.*;
import com.cobamovil.backend.repository.BookingRepository;
import com.cobamovil.backend.repository.PetRepository;
import com.cobamovil.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final RouteOptimizationService routeOptimizationService;

    public BookingService(BookingRepository bookingRepository, PetRepository petRepository, UserRepository userRepository, RouteOptimizationService routeOptimizationService) {
        this.bookingRepository = bookingRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.routeOptimizationService = routeOptimizationService;
    }

    @Transactional
    public BookingResponseDTO create(BookingCreateDTO dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new EntityNotFoundException("Pet not found"));
        if (!pet.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Pet does not belong to user");
        }
        Booking booking = new Booking();
        booking.setCustomer(user);
        booking.setPet(pet);
        booking.setServiceType(dto.getServiceType());
        booking.setDate(dto.getDate());
        booking.setTime(dto.getTime());
        booking.setAddress(dto.getAddress());
        booking.setLatitude(dto.getLatitude());
        booking.setLongitude(dto.getLongitude());
        booking.setNotes(dto.getNotes());
        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> listForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return bookingRepository.findByCustomerOrderByDateAscTimeAsc(user)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> listForDay(LocalDate date) {
        return bookingRepository.findByDate(date).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> optimizedIdsForDay(LocalDate date) {
        var approved = bookingRepository.findByDateAndStatus(date, BookingStatus.APPROVED);
        var ordered = routeOptimizationService.orderByNearest(approved);
        return ordered.stream().map(Booking::getId).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO updateStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        booking.setStatus(status);
        return toResponse(bookingRepository.save(booking));
    }

    private BookingResponseDTO toResponse(Booking b) {
        BookingResponseDTO r = new BookingResponseDTO();
        r.setId(b.getId());
        r.setPetId(b.getPet().getId());
        r.setPetName(b.getPet().getName());
        r.setServiceType(b.getServiceType());
        r.setDate(b.getDate());
        r.setTime(b.getTime());
        r.setAddress(b.getAddress());
        r.setLatitude(b.getLatitude());
        r.setLongitude(b.getLongitude());
        r.setStatus(b.getStatus());
        r.setNotes(b.getNotes());
        return r;
    }
}
