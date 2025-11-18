package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.BookingCreateDTO;
import com.cobamovil.backend.dto.BookingResponseDTO;
import com.cobamovil.backend.dto.AvailabilityResponseDTO;
import com.cobamovil.backend.entity.BookingStatus;
import com.cobamovil.backend.entity.ServiceType;
import com.cobamovil.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> create(@Valid @RequestBody BookingCreateDTO dto, Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(bookingService.create(dto, username));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> myBookings(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(bookingService.listForUser(username));
    }

    @GetMapping("/admin/day")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GROOMER') or hasRole('GROOMER')")
    public ResponseEntity<List<BookingResponseDTO>> listForDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.listForDay(date));
    }

    @GetMapping("/admin/route")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GROOMER') or hasRole('GROOMER')")
    public ResponseEntity<com.cobamovil.backend.dto.RoutePlanDTO> optimizedRoute(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                                 @RequestParam(required = false) Long groomerId) {
        var ids = groomerId == null ? bookingService.optimizedIdsForDay(date) : bookingService.optimizedIdsForDayAndGroomer(date, groomerId);
        var etas = bookingService.etasForOrderedIds(ids);
        return ResponseEntity.ok(new com.cobamovil.backend.dto.RoutePlanDTO(date.toString(), ids, etas));
    }

    @PutMapping("/admin/route")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GROOMER') or hasRole('GROOMER')")
    public ResponseEntity<Void> saveRoute(@RequestBody com.cobamovil.backend.dto.RoutePlanDTO dto) {
        bookingService.saveRoutePlan(LocalDate.parse(dto.getDate()), dto.getBookingIdsInOrder());
        return ResponseEntity.noContent().build();
    }

    // Start daily route for a groomer (generate plan + ETAs)
    @PostMapping("/route/start")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GROOMER') or hasRole('GROOMER')")
    public ResponseEntity<com.cobamovil.backend.dto.RoutePlanDTO> startRoute(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                             @RequestParam(required = false) Long groomerId) {
        var ids = groomerId == null ? bookingService.optimizedIdsForDay(date) : bookingService.optimizedIdsForDayAndGroomer(date, groomerId);
        bookingService.saveRoutePlan(date, ids);
        var etas = bookingService.etasForOrderedIds(ids);
        return ResponseEntity.ok(new com.cobamovil.backend.dto.RoutePlanDTO(date.toString(), ids, etas));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GROOMER') or hasRole('GROOMER')")
    public ResponseEntity<BookingResponseDTO> updateStatus(@PathVariable Long id, @RequestParam BookingStatus status) {
        return ResponseEntity.ok(bookingService.updateStatus(id, status));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<BookingResponseDTO> reschedule(Authentication auth,
                                                         @PathVariable Long id,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                                                         @RequestParam(required = false) ServiceType serviceType) {
        return ResponseEntity.ok(bookingService.reschedule(id, auth.getName(), date, time, serviceType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication auth, @PathVariable Long id) {
        bookingService.cancel(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponseDTO> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam ServiceType serviceType
    ) {
        return ResponseEntity.ok(bookingService.checkAvailability(date, time, serviceType));
    }
}
