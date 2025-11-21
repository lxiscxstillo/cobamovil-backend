package com.cobamovil.backend.dto;

public class CareWeekPlanResponseDTO {

    private String planText;
    private String generatedAt;

    public CareWeekPlanResponseDTO() {
    }

    public CareWeekPlanResponseDTO(String planText, String generatedAt) {
        this.planText = planText;
        this.generatedAt = generatedAt;
    }

    public String getPlanText() {
        return planText;
    }

    public void setPlanText(String planText) {
        this.planText = planText;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}

