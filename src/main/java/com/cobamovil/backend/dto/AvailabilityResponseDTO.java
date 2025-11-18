package com.cobamovil.backend.dto;

import java.util.List;

public class AvailabilityResponseDTO {
    private boolean available;
    private String message;
    private List<Long> groomerIds;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Long> getGroomerIds() { return groomerIds; }
    public void setGroomerIds(List<Long> groomerIds) { this.groomerIds = groomerIds; }
}

