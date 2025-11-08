package com.cobamovil.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DistanceMatrixService {
    private static final Logger log = LoggerFactory.getLogger(DistanceMatrixService.class);
    private final RestTemplate http = new RestTemplate();
    private final String apiKey = System.getenv("GOOGLE_MAPS_API_KEY");

    public Integer durationMinutes(double oLat, double oLng, double dLat, double dLng) {
        try {
            if (apiKey == null || apiKey.isBlank()) return null;
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&mode=driving&departure_time=now&key=%s",
                    oLat, oLng, dLat, dLng, apiKey);
            ResponseEntity<java.util.Map> resp = http.getForEntity(url, java.util.Map.class);
            var body = resp.getBody();
            if (body == null) return null;
            var rows = (java.util.List<?>) body.get("rows");
            if (rows == null || rows.isEmpty()) return null;
            var elements = (java.util.List<?>) ((java.util.Map<?,?>)rows.get(0)).get("elements");
            if (elements == null || elements.isEmpty()) return null;
            var el = (java.util.Map<?,?>) elements.get(0);
            var duration = (java.util.Map<?,?>) el.get("duration");
            if (duration == null) duration = (java.util.Map<?,?>) el.get("duration_in_traffic");
            if (duration == null) return null;
            Number seconds = (Number) duration.get("value");
            if (seconds == null) return null;
            return (int) Math.round(seconds.doubleValue() / 60.0);
        } catch (Exception ex) {
            log.warn("DistanceMatrix error: {}", ex.getMessage());
            return null;
        }
    }
}

