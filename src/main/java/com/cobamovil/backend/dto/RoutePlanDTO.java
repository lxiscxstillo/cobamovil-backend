package com.cobamovil.backend.dto;

import java.util.List;

public class RoutePlanDTO {
    private String date; // ISO yyyy-MM-dd
    private List<Long> bookingIdsInOrder;
    private List<Integer> etasMinutes; // optional: cumulative ETA per stop in minutes

    public RoutePlanDTO() {}
    public RoutePlanDTO(String date, List<Long> ids) { this.date = date; this.bookingIdsInOrder = ids; }
    public RoutePlanDTO(String date, List<Long> ids, List<Integer> etas) { this.date = date; this.bookingIdsInOrder = ids; this.etasMinutes = etas; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<Long> getBookingIdsInOrder() { return bookingIdsInOrder; }
    public void setBookingIdsInOrder(List<Long> bookingIdsInOrder) { this.bookingIdsInOrder = bookingIdsInOrder; }
    public List<Integer> getEtasMinutes() { return etasMinutes; }
    public void setEtasMinutes(List<Integer> etasMinutes) { this.etasMinutes = etasMinutes; }
}
