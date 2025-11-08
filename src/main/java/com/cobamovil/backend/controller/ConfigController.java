package com.cobamovil.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicConfig() {
        // Exposing Maps key is safe only with proper HTTP referrer restrictions
        String mapsKey = System.getenv("GOOGLE_MAPS_API_KEY");
        return ResponseEntity.ok(Map.of(
                "googleMapsApiKey", mapsKey == null ? "" : mapsKey
        ));
    }
}

