package com.trekmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Entity representing an expedition batch date and slot capacity.
 */
@Entity
@Table(name = "trek_dates")
public class TrekDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trek_id", nullable = false)
    private Trek trek;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Total slots are required")
    @Min(value = 1, message = "Total slots must be at least 1")
    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots = 20;

    @Column(name = "booked_slots", nullable = false)
    private Integer bookedSlots = 0;

    @Column(nullable = false, length = 20)
    private String status = "OPEN"; // OPEN, FULL, CANCELLED

    public TrekDate() {
    }

    public TrekDate(Trek trek, LocalDate startDate, LocalDate endDate, Integer totalSlots) {
        this.trek = trek;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalSlots = totalSlots;
        this.bookedSlots = 0;
        this.status = "OPEN";
    }

    // Business helpers
    public int getAvailableSlots() {
        return Math.max(0, totalSlots - (bookedSlots != null ? bookedSlots : 0));
    }

    public boolean hasCapacityFor(int requestedSlots) {
        return "OPEN".equalsIgnoreCase(status) && getAvailableSlots() >= requestedSlots;
    }

    public void bookSlots(int count) {
        if (this.bookedSlots == null) {
            this.bookedSlots = 0;
        }
        this.bookedSlots += count;
        if (this.bookedSlots >= this.totalSlots) {
            this.status = "FULL";
        }
    }

    public void releaseSlots(int count) {
        if (this.bookedSlots != null) {
            this.bookedSlots = Math.max(0, this.bookedSlots - count);
            if (this.bookedSlots < this.totalSlots && "FULL".equalsIgnoreCase(this.status)) {
                this.status = "OPEN";
            }
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trek getTrek() {
        return trek;
    }

    public void setTrek(Trek trek) {
        this.trek = trek;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Integer getBookedSlots() {
        return bookedSlots;
    }

    public void setBookedSlots(Integer bookedSlots) {
        this.bookedSlots = bookedSlots;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

