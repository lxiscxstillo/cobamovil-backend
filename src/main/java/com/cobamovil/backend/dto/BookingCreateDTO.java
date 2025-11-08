package com.cobamovil.backend.dto;

import com.cobamovil.backend.entity.ServiceType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class BookingCreateDTO {
    @NotNull
    private Long petId;
    @NotNull
    private ServiceType serviceType;
    private Long groomerId; // optional selected groomer
    @NotNull @Future
    private LocalDate date;
    @NotNull
    private LocalTime time;
    private String address;
    private Double latitude;
    private Double longitude;
    private String notes;

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getGroomerId() { return groomerId; }
    public void setGroomerId(Long groomerId) { this.groomerId = groomerId; }
}
