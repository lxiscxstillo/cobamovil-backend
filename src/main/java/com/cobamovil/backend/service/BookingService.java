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
import java.time.LocalTime;
import java.time.LocalDateTime;
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
    private final com.cobamovil.backend.repository.CutRecordRepository cutRecordRepository;
    private final DistanceMatrixService distanceMatrixService;

    public BookingService(BookingRepository bookingRepository,
                          PetRepository petRepository,
                          UserRepository userRepository,
                          RouteOptimizationService routeOptimizationService,
                          CoverageAreaService coverageAreaService,
                          NotificationService notificationService,
                          RoutePlanRepository routePlanRepository,
                          com.cobamovil.backend.repository.CutRecordRepository cutRecordRepository,
                          DistanceMatrixService distanceMatrixService) {
        this.bookingRepository = bookingRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.routeOptimizationService = routeOptimizationService;
        this.coverageAreaService = coverageAreaService;
        this.notificationService = notificationService;
        this.routePlanRepository = routePlanRepository;
        this.cutRecordRepository = cutRecordRepository;
        this.distanceMatrixService = distanceMatrixService;
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
        // Assign a groomer: use selection when provided; fallback to first available
        if (dto.getGroomerId() != null) {
            User g = userRepository.findById(dto.getGroomerId()).orElseThrow(() -> new EntityNotFoundException("Groomer not found"));
            if (!"GROOMER".equalsIgnoreCase(g.getRole())) throw new IllegalArgumentException("Selected user is not a groomer");
            booking.setAssignedGroomer(g);
            notificationService.notifyBookingEvent(g, "BOOKING_CREATED", "WHATSAPP");
        } else {
            var groomers = userRepository.findByRole("GROOMER");
            if (groomers != null && !groomers.isEmpty()) {
                booking.setAssignedGroomer(groomers.get(0));
                notificationService.notifyBookingEvent(groomers.get(0), "BOOKING_CREATED", "WHATSAPP");
            }
        }
        Booking saved = bookingRepository.save(booking);
        // Optionally notify customer that request is pending
        notificationService.notifyBookingEvent(user, "BOOKING_CREATED", "EMAIL");
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
        // Si el usuario autenticado es un peluquero, solo devolvemos las reservas asignadas a él.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && "GROOMER".equalsIgnoreCase(user.getRole())) {
                return bookingRepository.findByDate(date).stream()
                        .filter(b -> b.getAssignedGroomer() != null && b.getAssignedGroomer().getId().equals(user.getId()))
                        .map(this::toResponse)
                        .collect(Collectors.toList());
            }
        }
        // Para ADMIN u otros roles, devolvemos todas las reservas del día.
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

    @Transactional(readOnly = true)
    public List<Long> optimizedIdsForDayAndGroomer(LocalDate date, Long groomerId) {
        var list = bookingRepository.findByDateAndStatus(date, BookingStatus.APPROVED)
                .stream().filter(b -> b.getAssignedGroomer() != null &&
                        b.getAssignedGroomer().getId().equals(groomerId)).toList();
        var ordered = routeOptimizationService.orderByNearest(list);
        return ordered.stream().map(Booking::getId).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.List<Integer> etasForOrderedIds(java.util.List<Long> ids) {
        java.util.List<Integer> etas = new java.util.ArrayList<>();
        int cum = 0;
        if (ids == null || ids.size() < 2) { if (ids != null) for (int i=0;i<ids.size();i++) etas.add(cum); return etas; }
        java.util.Map<Long, Booking> map = bookingRepository.findAllById(ids).stream().collect(java.util.stream.Collectors.toMap(Booking::getId, b -> b));
        for (int i=0;i<ids.size();i++) {
            if (i==0) { etas.add(cum); continue; }
            Booking prev = map.get(ids.get(i-1));
            Booking curr = map.get(ids.get(i));
            Integer minutes = distanceMatrixService.durationMinutes(s(prev.getLatitude()), s(prev.getLongitude()), s(curr.getLatitude()), s(curr.getLongitude()));
            if (minutes == null) minutes = approxMinutes(prev, curr);
            cum += minutes;
            etas.add(cum);
        }
        return etas;
    }

    private static double s(Double v) { return v == null ? 0.0 : v; }
    private static int approxMinutes(Booking a, Booking b) {
        double lat1 = s(a.getLatitude()), lon1 = s(a.getLongitude());
        double lat2 = s(b.getLatitude()), lon2 = s(b.getLongitude());
        // haversine distance in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double aa = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1-aa));
        double km = 6371 * c;
        return (int)Math.round((km / 30.0) * 60.0); // assume 30km/h avg
    }

    @Transactional
    public BookingResponseDTO updateStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        // Si el usuario autenticado es un peluquero, solo puede modificar reservas asignadas a �l.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && "GROOMER".equalsIgnoreCase(user.getRole())) {
                if (booking.getAssignedGroomer() == null || !booking.getAssignedGroomer().getId().equals(user.getId())) {
                    throw new IllegalStateException("No puedes modificar reservas que no est�n asignadas a tu perfil de peluquero.");
                }
            }
        }
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);
        User user = saved.getCustomer();
        switch (status) {
            case APPROVED -> notificationService.notifyBookingEvent(user, "BOOKING_APPROVED", "WHATSAPP");
            case REJECTED -> notificationService.notifyBookingEvent(user, "BOOKING_REJECTED", "WHATSAPP");
            case ON_ROUTE -> notificationService.notifyBookingEvent(user, "BOOKING_ON_ROUTE", "WHATSAPP");
            case COMPLETED -> {
                notificationService.notifyBookingEvent(user, "BOOKING_COMPLETED", "EMAIL");
                // Create a cut record for groomer
                if (saved.getAssignedGroomer() != null) {
                    com.cobamovil.backend.entity.CutRecord rec = new com.cobamovil.backend.entity.CutRecord();
                    rec.setGroomer(saved.getAssignedGroomer());
                    rec.setServiceType(saved.getServiceType());
                    rec.setPetName(saved.getPet().getName());
                    rec.setDate(saved.getDate());
                    rec.setTime(saved.getTime());
                    rec.setNotes(saved.getNotes());
                    cutRecordRepository.save(rec);
                }
            }
            default -> {}
        }
        return toResponse(saved);
    }

    @Transactional
    public BookingResponseDTO reschedule(Long id, String username, LocalDate date, java.time.LocalTime time, ServiceType serviceType) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (!b.getCustomer().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this booking");
        }
        if (b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.REJECTED) {
            throw new IllegalStateException("Cannot reschedule completed or rejected booking");
        }
        validateAvailability(date, time, serviceType != null ? serviceType : b.getServiceType());
        if (serviceType != null) b.setServiceType(serviceType);
        b.setDate(date);
        b.setTime(time);
        // After rescheduling, booking should go back to pending approval
        b.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(b);
        notificationService.notifyBookingEvent(saved.getCustomer(), "BOOKING_RESCHEDULED", "EMAIL");
        return toResponse(saved);
    }

    @Transactional
    public void cancel(Long id, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (!b.getCustomer().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this booking");
        }
        // Regla nueva: mientras la cita est├® pendiente, siempre se puede cancelar.
        // Si ya fue aceptada o est├í en otro estado, no se permite cancelarla.
        if (b.getStatus() != BookingStatus.PENDING) {
            // userMessage: el usuario entiende claramente por qu├® no puede cancelar
            throw new IllegalStateException("Esta cita ya fue aceptada por el peluquero y no puede modificarse.");
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

    @Transactional(readOnly = true)
    public com.cobamovil.backend.dto.AvailabilityResponseDTO checkAvailability(LocalDate date, LocalTime time, ServiceType serviceType) {
        com.cobamovil.backend.dto.AvailabilityResponseDTO dto = new com.cobamovil.backend.dto.AvailabilityResponseDTO();
        // Date/time in the past
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = LocalDateTime.of(date, time);
        if (candidate.isBefore(now)) {
            dto.setAvailable(false);
            dto.setMessage("La hora que elegiste ya pas├â┬│. Por favor selecciona otra hora disponible.");
            dto.setGroomerIds(java.util.Collections.emptyList());
            return dto;
        }
        try {
            validateAvailability(date, time, serviceType);
        } catch (IllegalStateException ex) {
            dto.setAvailable(false);
            dto.setMessage("Ese horario ya ha sido reservado. Por favor, elige otro.");
            dto.setGroomerIds(java.util.Collections.emptyList());
            return dto;
        }
        var groomers = userRepository.findByRole("GROOMER");
        if (groomers == null || groomers.isEmpty()) {
            dto.setAvailable(false);
            dto.setMessage("No hay peluqueros disponibles en ese horario. Intenta otra fecha u hora.");
            dto.setGroomerIds(java.util.Collections.emptyList());
            return dto;
        }
        dto.setAvailable(true);
        dto.setMessage("Horario disponible.");
        dto.setGroomerIds(groomers.stream().map(User::getId).collect(java.util.stream.Collectors.toList()));
        return dto;
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



