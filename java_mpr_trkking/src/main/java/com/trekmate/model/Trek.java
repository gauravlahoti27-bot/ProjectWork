package com.trekmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an expedition / trek package.
 */
@Entity
@Table(name = "treks")
public class Trek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Trek title is required")
    @Column(nullable = false, length = 120)
    private String title;

    @NotBlank(message = "Destination is required")
    @Column(nullable = false, length = 100)
    private String destination;

    @Column(length = 60)
    private String state;

    @NotBlank(message = "Difficulty level is required")
    @Column(nullable = false, length = 30)
    private String difficulty; // Easy, Moderate, Challenging, Difficult

    @NotNull(message = "Duration in days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @NotNull(message = "Duration in nights is required")
    @Min(value = 0, message = "Duration nights cannot be negative")
    @Column(name = "duration_nights", nullable = false)
    private Integer durationNights;

    @NotNull(message = "Base price per person is required")
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "altitude_meters")
    private Integer altitudeMeters;

    @Column(name = "fitness_level", length = 50)
    private String fitnessLevel; // Beginner, Average, High, Expert

    @Column(name = "best_season", length = 100)
    private String bestSeason;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "trek", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrekDate> availableDates = new ArrayList<>();

    public Trek() {
    }

    public Trek(String title, String destination, String state, String difficulty, Integer durationDays,
                Integer durationNights, BigDecimal basePrice, String description, Integer altitudeMeters,
                String fitnessLevel, String bestSeason, String imageUrl) {
        this.title = title;
        this.destination = destination;
        this.state = state;
        this.difficulty = difficulty;
        this.durationDays = durationDays;
        this.durationNights = durationNights;
        this.basePrice = basePrice;
        this.description = description;
        this.altitudeMeters = altitudeMeters;
        this.fitnessLevel = fitnessLevel;
        this.bestSeason = bestSeason;
        this.imageUrl = imageUrl;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Integer getDurationNights() {
        return durationNights;
    }

    public void setDurationNights(Integer durationNights) {
        this.durationNights = durationNights;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(Integer altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public String getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(String fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }

    public String getBestSeason() {
        return bestSeason;
    }

    public void setBestSeason(String bestSeason) {
        this.bestSeason = bestSeason;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<TrekDate> getAvailableDates() {
        return availableDates;
    }

    public void setAvailableDates(List<TrekDate> availableDates) {
        this.availableDates = availableDates;
    }

    public String getDurationFormatted() {
        return durationDays + "D / " + durationNights + "N";
    }
}

