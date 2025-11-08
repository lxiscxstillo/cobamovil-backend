package com.cobamovil.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "cut_records")
public class CutRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "groomer_id", nullable = false)
    private User groomer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceType serviceType;

    @Column(length = 60)
    private String petName;

    private LocalDate date;
    private LocalTime time;

    @Column(length = 500)
    private String notes;

    @Column(length = 255)
    private String photoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getGroomer() { return groomer; }
    public void setGroomer(User groomer) { this.groomer = groomer; }
    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}

