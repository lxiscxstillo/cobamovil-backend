package com.cobamovil.backend.service;

import com.cobamovil.backend.config.CoverageProperties;
import org.springframework.stereotype.Service;

@Service
public class CoverageAreaService {
    private final CoverageProperties props;

    public CoverageAreaService(CoverageProperties props) {
        this.props = props;
    }

    public boolean isWithinCoverage(Double lat, Double lng) {
        if (!props.isEnabled()) return true; // if disabled, accept all
        if (lat == null || lng == null) return false;
        return lat >= props.getMinLat() && lat <= props.getMaxLat()
                && lng >= props.getMinLng() && lng <= props.getMaxLng();
    }
}

