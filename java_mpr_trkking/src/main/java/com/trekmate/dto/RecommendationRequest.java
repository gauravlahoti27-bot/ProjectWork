package com.trekmate.dto;

import java.math.BigDecimal;

public class RecommendationRequest {
    private BigDecimal budget;
    private Integer groupSize = 1;
    private String difficulty; // "ANY", "EASY", "MODERATE", "CHALLENGING", "DIFFICULT"
    private String destination;
    private Integer maxDuration;

    public RecommendationRequest() {
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Integer getGroupSize() {
        return groupSize != null && groupSize > 0 ? groupSize : 1;
    }

    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
    }
}

