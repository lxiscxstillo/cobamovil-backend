package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.BookingCreateDTO;
import com.cobamovil.backend.dto.BookingResponseDTO;
import com.cobamovil.backend.entity.*;
import com.cobamovil.backend.repository.BookingRepository;
import com.cobamovil.backend.repository.RoutePlanRepository;
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
    private final CoverageAreaService coverageAreaService;
    private final NotificationService notificationService;
    private final RoutePlanRepository routePlanRepository;

    public BookingService(BookingRepository bookingRepository,
                          PetRepository petRepository,
                          UserRepository userRepository,
                          RouteOptimizationService routeOptimizationService,
                          CoverageAreaService coverageAreaService,
                          NotificationService notificationService,
                          RoutePlanRepository routePlanRepository) {
        this.bookingRepository = bookingRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.routeOptimizationService = routeOptimizationService;
        this.coverageAreaService = coverageAreaService;
        this.notificationService = notificationService;
        this.routePlanRepository = routePlanRepository;
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
        // Basic validations
        if (dto.getLatitude() != null && dto.getLongitude() != null &&
                !coverageAreaService.isWithinCoverage(dto.getLatitude(), dto.getLongitude())) {
            throw new IllegalArgumentException("Address out of coverage area");
        }
        // Validate overlap with approved bookings (single groomer assumption)
        validateAvailability(dto.getDate(), dto.getTime(), dto.getServiceType());
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
        notificationService.notifyBookingEvent(user, "BOOKING_CREATED", "INTERNAL");
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
        // If a manual plan exists, honor it
        var planOpt = routePlanRepository.findByDate(date);
        if (planOpt.isPresent() && planOpt.get().getOrderCsv() != null && !planOpt.get().getOrderCsv().isBlank()) {
            var map = approved.stream().collect(Collectors.toMap(Booking::getId, b -> b));
            var ids = List.of(planOpt.get().getOrderCsv().split(","))
                    .stream().filter(s -> !s.isBlank()).map(Long::valueOf).toList();
            // Only include those still approved
            return ids.stream().filter(map::containsKey).collect(Collectors.toList());
        }
        var ordered = routeOptimizationService.orderByNearest(approved);
        return ordered.stream().map(Booking::getId).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO updateStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);
        User user = saved.getCustomer();
        switch (status) {
            case APPROVED -> notificationService.notifyBookingEvent(user, "BOOKING_APPROVED", "WHATSAPP");
            case REJECTED -> notificationService.notifyBookingEvent(user, "BOOKING_REJECTED", "WHATSAPP");
            case ON_ROUTE -> notificationService.notifyBookingEvent(user, "BOOKING_ON_ROUTE", "WHATSAPP");
            case COMPLETED -> notificationService.notifyBookingEvent(user, "BOOKING_COMPLETED", "EMAIL");
            default -> {}
        }
        return toResponse(saved);
    }

    @Transactional
    public BookingResponseDTO reschedule(Long id, String username, LocalDate date, java.time.LocalTime time, ServiceType serviceType) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (!b.getCustomer().getId().equals(user.getId())) throw new IllegalArgumentException("Unauthorized");
        if (b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.REJECTED) {
            throw new IllegalStateException("Cannot reschedule completed or rejected booking");
        }
        validateAvailability(date, time, serviceType != null ? serviceType : b.getServiceType());
        if (serviceType != null) b.setServiceType(serviceType);
        b.setDate(date);
        b.setTime(time);
        Booking saved = bookingRepository.save(b);
        notificationService.notifyBookingEvent(saved.getCustomer(), "BOOKING_RESCHEDULED", "EMAIL");
        return toResponse(saved);
    }

    @Transactional
    public void cancel(Long id, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (!b.getCustomer().getId().equals(user.getId())) throw new IllegalArgumentException("Unauthorized");
        // Allow cancel only before 12 hours of appointment
        java.time.LocalDateTime appointment = java.time.LocalDateTime.of(b.getDate(), b.getTime());
        if (java.time.Duration.between(java.time.LocalDateTime.now(), appointment).toHours() < 12) {
            throw new IllegalStateException("Too late to cancel");
        }
        b.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(b);
        notificationService.notifyBookingEvent(b.getCustomer(), "BOOKING_CANCELED", "WHATSAPP");
    }

    @Transactional
    public void saveRoutePlan(LocalDate date, java.util.List<Long> idsInOrder) {
        var plan = routePlanRepository.findByDate(date).orElseGet(() -> { var p = new com.cobamovil.backend.entity.RoutePlan(); p.setDate(date); return p; });
        String csv = idsInOrder == null ? "" : idsInOrder.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        plan.setOrderCsv(csv);
        routePlanRepository.save(plan);
    }

    private void validateAvailability(LocalDate date, java.time.LocalTime time, ServiceType serviceType) {
        int duration = switch (serviceType) {
            case BATH -> 45; // minutes
            case HAIRCUT -> 60;
            case NAIL_TRIM -> 20;
            case FULL_GROOMING -> 90;
        };
        java.time.LocalDateTime start = java.time.LocalDateTime.of(date, time);
        java.time.LocalDateTime end = start.plusMinutes(duration);
        var approved = bookingRepository.findByDateAndStatus(date, BookingStatus.APPROVED);
        for (Booking other : approved) {
            java.time.LocalDateTime oStart = java.time.LocalDateTime.of(other.getDate(), other.getTime());
            int oDur = switch (other.getServiceType()) {
                case BATH -> 45; case HAIRCUT -> 60; case NAIL_TRIM -> 20; case FULL_GROOMING -> 90; };
            java.time.LocalDateTime oEnd = oStart.plusMinutes(oDur);
            boolean overlap = !end.isBefore(oStart) && !start.isAfter(oEnd);
            if (overlap) throw new IllegalStateException("Selected time overlaps with another approved booking");
        }
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
