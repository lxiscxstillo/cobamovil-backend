package com.cobamovil.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class NormalizeNotesRequestDTO {

    @NotBlank
    private String rawText;

    @NotBlank
    private String context; // expected: SALUD or COMPORTAMIENTO

    public NormalizeNotesRequestDTO() {
    }

    public NormalizeNotesRequestDTO(String rawText, String context) {
        this.rawText = rawText;
        this.context = context;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}

