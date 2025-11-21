package com.cobamovil.backend.dto;

public class NormalizeNotesResponseDTO {

    private String normalizedText;

    public NormalizeNotesResponseDTO() {
    }

    public NormalizeNotesResponseDTO(String normalizedText) {
        this.normalizedText = normalizedText;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public void setNormalizedText(String normalizedText) {
        this.normalizedText = normalizedText;
    }
}

