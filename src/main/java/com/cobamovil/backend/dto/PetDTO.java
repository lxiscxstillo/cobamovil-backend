package com.cobamovil.backend.dto;

public class PetDTO {
    private Long id;
    private String name;
    private String breed;
    private String sex;
    private Integer age;
    private Double weight;
    private String behavior;
    private String healthNotes;
    private String vaccinations;
    private String deworming;
    private String medicalConditions;
    private String lastGroomDate; // ISO yyyy-MM-dd

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getLastGroomDate() { return lastGroomDate; }
    public void setLastGroomDate(String lastGroomDate) { this.lastGroomDate = lastGroomDate; }
}
