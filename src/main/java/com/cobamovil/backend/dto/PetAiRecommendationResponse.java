package com.cobamovil.backend.dto;

public class PetAiRecommendationResponse {

    private Long petId;
    private String petName;
    private String recommendedServiceType; // e.g. BATH, FULL_GROOMING
    private String recommendedFrequency;   // e.g. "Cada 4–6 semanas"
    private String advice;                 // main text for the user

    public PetAiRecommendationResponse() {
    }

    public PetAiRecommendationResponse(Long petId,
                                       String petName,
                                       String recommendedServiceType,
                                       String recommendedFrequency,
                                       String advice) {
        this.petId = petId;
        this.petName = petName;
        this.recommendedServiceType = recommendedServiceType;
        this.recommendedFrequency = recommendedFrequency;
        this.advice = advice;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getRecommendedServiceType() {
        return recommendedServiceType;
    }

    public void setRecommendedServiceType(String recommendedServiceType) {
        this.recommendedServiceType = recommendedServiceType;
    }

    public String getRecommendedFrequency() {
        return recommendedFrequency;
    }

    public void setRecommendedFrequency(String recommendedFrequency) {
        this.recommendedFrequency = recommendedFrequency;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }
}

