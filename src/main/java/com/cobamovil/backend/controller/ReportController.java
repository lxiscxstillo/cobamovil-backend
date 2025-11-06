package com.cobamovil.backend.controller;

import com.cobamovil.backend.entity.BookingStatus;
import com.cobamovil.backend.entity.ServiceType;
import com.cobamovil.backend.repository.BookingRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
public class ReportController {
    private final BookingRepository bookingRepository;

    public ReportController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/services-summary")
    public ResponseEntity<Map<String, Object>> servicesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Long> perService = new LinkedHashMap<>();
        for (ServiceType st : ServiceType.values()) perService.put(st.name(), 0L);
        long totalCompleted = 0L;
        LocalDate day = from;
        while (!day.isAfter(to)) {
            var list = bookingRepository.findByDate(day);
            for (var b : list) {
                if (b.getStatus() == BookingStatus.COMPLETED) {
                    totalCompleted++;
                    perService.compute(b.getServiceType().name(), (k, v) -> v + 1);
                }
            }
            day = day.plusDays(1);
        }
        out.put("totalCompleted", totalCompleted);
        out.put("byServiceType", perService);
        return ResponseEntity.ok(out);
    }
}

