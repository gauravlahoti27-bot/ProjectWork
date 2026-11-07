package com.trekmate.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CostCalculationResponse {
    private BigDecimal baseCost = BigDecimal.ZERO;
    private BigDecimal activitiesCost = BigDecimal.ZERO;
    private BigDecimal facilitiesCost = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal perPersonCost = BigDecimal.ZERO;
    private BigDecimal userBudget = BigDecimal.ZERO;
    private BigDecimal budgetDiff = BigDecimal.ZERO;
    private boolean withinBudget = true;
    private List<String> suggestions = new ArrayList<>();
    private List<CheaperAlternativeDto> alternativeTreks = new ArrayList<>();

    public static class CheaperAlternativeDto {
        private Long id;
        private String title;
        private String destination;
        private String difficulty;
        private BigDecimal basePrice;
        private BigDecimal totalForGroup;
        private BigDecimal savings;

        public CheaperAlternativeDto() {}

        public CheaperAlternativeDto(Long id, String title, String destination, String difficulty,
                                     BigDecimal basePrice, BigDecimal totalForGroup, BigDecimal savings) {
            this.id = id;
            this.title = title;
            this.destination = destination;
            this.difficulty = difficulty;
            this.basePrice = basePrice;
            this.totalForGroup = totalForGroup;
            this.savings = savings;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
        public BigDecimal getTotalForGroup() { return totalForGroup; }
        public void setTotalForGroup(BigDecimal totalForGroup) { this.totalForGroup = totalForGroup; }
        public BigDecimal getSavings() { return savings; }
        public void setSavings(BigDecimal savings) { this.savings = savings; }
    }

    public CostCalculationResponse() {
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getActivitiesCost() {
        return activitiesCost;
    }

    public void setActivitiesCost(BigDecimal activitiesCost) {
        this.activitiesCost = activitiesCost;
    }

    public BigDecimal getFacilitiesCost() {
        return facilitiesCost;
    }

    public void setFacilitiesCost(BigDecimal facilitiesCost) {
        this.facilitiesCost = facilitiesCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getPerPersonCost() {
        return perPersonCost;
    }

    public void setPerPersonCost(BigDecimal perPersonCost) {
        this.perPersonCost = perPersonCost;
    }

    public BigDecimal getUserBudget() {
        return userBudget;
    }

    public void setUserBudget(BigDecimal userBudget) {
        this.userBudget = userBudget;
    }

    public BigDecimal getBudgetDiff() {
        return budgetDiff;
    }

    public void setBudgetDiff(BigDecimal budgetDiff) {
        this.budgetDiff = budgetDiff;
    }

    public boolean isWithinBudget() {
        return withinBudget;
    }

    public void setWithinBudget(boolean withinBudget) {
        this.withinBudget = withinBudget;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<CheaperAlternativeDto> getAlternativeTreks() {
        return alternativeTreks;
    }

    public void setAlternativeTreks(List<CheaperAlternativeDto> alternativeTreks) {
        this.alternativeTreks = alternativeTreks;
    }
}

