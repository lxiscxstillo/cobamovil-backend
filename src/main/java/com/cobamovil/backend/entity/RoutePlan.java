package com.cobamovil.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "route_plans", uniqueConstraints = @UniqueConstraint(columnNames = {"date"}))
public class RoutePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "order_csv", length = 4000)
    private String orderCsv; // comma-separated booking IDs

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getOrderCsv() { return orderCsv; }
    public void setOrderCsv(String orderCsv) { this.orderCsv = orderCsv; }
}

