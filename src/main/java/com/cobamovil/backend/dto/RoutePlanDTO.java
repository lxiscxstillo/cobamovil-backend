package com.cobamovil.backend.dto;

import java.util.List;

public class RoutePlanDTO {
    private String date; // ISO yyyy-MM-dd
    private List<Long> bookingIdsInOrder;

    public RoutePlanDTO() {}
    public RoutePlanDTO(String date, List<Long> ids) { this.date = date; this.bookingIdsInOrder = ids; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<Long> getBookingIdsInOrder() { return bookingIdsInOrder; }
    public void setBookingIdsInOrder(List<Long> bookingIdsInOrder) { this.bookingIdsInOrder = bookingIdsInOrder; }
}

