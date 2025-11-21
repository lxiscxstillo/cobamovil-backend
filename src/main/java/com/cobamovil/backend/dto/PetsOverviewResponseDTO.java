package com.cobamovil.backend.dto;

public class PetsOverviewResponseDTO {

    private String overviewText;
    private int totalPets;

    public PetsOverviewResponseDTO() {
    }

    public PetsOverviewResponseDTO(String overviewText, int totalPets) {
        this.overviewText = overviewText;
        this.totalPets = totalPets;
    }

    public String getOverviewText() {
        return overviewText;
    }

    public void setOverviewText(String overviewText) {
        this.overviewText = overviewText;
    }

    public int getTotalPets() {
        return totalPets;
    }

    public void setTotalPets(int totalPets) {
        this.totalPets = totalPets;
    }
}

