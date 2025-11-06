package com.cobamovil.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.coverage")
public class CoverageProperties {
    private boolean enabled = false;
    private double minLat;
    private double maxLat;
    private double minLng;
    private double maxLng;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public double getMinLat() { return minLat; }
    public void setMinLat(double minLat) { this.minLat = minLat; }
    public double getMaxLat() { return maxLat; }
    public void setMaxLat(double maxLat) { this.maxLat = maxLat; }
    public double getMinLng() { return minLng; }
    public void setMinLng(double minLng) { this.minLng = minLng; }
    public double getMaxLng() { return maxLng; }
    public void setMaxLng(double maxLng) { this.maxLng = maxLng; }
}

