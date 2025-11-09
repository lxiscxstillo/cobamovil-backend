package com.cobamovil.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String name;

    @Size(max = 60)
    @Column(length = 60)
    private String breed;

    @Size(max = 10)
    @Column(length = 10)
    private String sex; // "M" | "F"

    @Column
    private Integer age; // in years

    @Column
    private Double weight; // in kg

    @Size(max = 255)
    private String behavior; // free text or tags

    @Size(max = 255)
    private String healthNotes; // allergies, conditions, etc.

    @Column(name = "vaccinations", columnDefinition = "TEXT")
    private String vaccinations; // vaccines info (names/dates)

    @Column(name = "deworming", columnDefinition = "TEXT")
    private String deworming; // deworming notes/dates

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions; // chronic conditions, allergies, etc.

    @Column(name = "last_groom_date")
    private java.time.LocalDate lastGroomDate; // last grooming date

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getBehavior() { return behavior; }
    public void setBehavior(String behavior) { this.behavior = behavior; }
    public String getHealthNotes() { return healthNotes; }
    public void setHealthNotes(String healthNotes) { this.healthNotes = healthNotes; }
    public String getVaccinations() { return vaccinations; }
    public void setVaccinations(String vaccinations) { this.vaccinations = vaccinations; }
    public String getDeworming() { return deworming; }
    public void setDeworming(String deworming) { this.deworming = deworming; }
    public String getMedicalConditions() { return medicalConditions; }
    public void setMedicalConditions(String medicalConditions) { this.medicalConditions = medicalConditions; }
    public java.time.LocalDate getLastGroomDate() { return lastGroomDate; }
    public void setLastGroomDate(java.time.LocalDate lastGroomDate) { this.lastGroomDate = lastGroomDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
