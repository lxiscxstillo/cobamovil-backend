package com.cobamovil.backend.service;

import com.cobamovil.backend.entity.Booking;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RouteOptimizationService {

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0 && lon1 == 0) return 0; // naive guard
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    /**
     * Greedy nearest-neighbor ordering by lat/lng. Assumes all bookings have coordinates.
     */
    public List<Booking> orderByNearest(List<Booking> bookings) {
        if (bookings == null || bookings.size() <= 1) return bookings;
        List<Booking> remaining = new ArrayList<>(bookings);
        List<Booking> ordered = new ArrayList<>();
        // Start at the first booking
        Booking current = remaining.remove(0);
        ordered.add(current);
        while (!remaining.isEmpty()) {
            final double latC = current.getLatitude() != null ? current.getLatitude() : 0;
            final double lonC = current.getLongitude() != null ? current.getLongitude() : 0;
            Booking next = remaining.stream()
                .min(Comparator.comparingDouble(b -> haversine(
                        latC,
                        lonC,
                        b.getLatitude() != null ? b.getLatitude() : 0,
                        b.getLongitude() != null ? b.getLongitude() : 0)))
                .orElse(remaining.get(0));
            remaining.remove(next);
            ordered.add(next);
            current = next;
        }
        return ordered;
    }
}
