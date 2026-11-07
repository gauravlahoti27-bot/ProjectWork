package com.trekmate.dto;

import com.trekmate.model.Trek;
import java.math.BigDecimal;

public class TrekRecommendationDto {
    private Trek trek;
    private BigDecimal totalBaseCostForGroup;
    private BigDecimal estimatedTotalWithEssentials;
    private BigDecimal remainingBudget;
    private boolean withinBudget;
    private int matchScore; // 0 to 100
    private String matchBadge; // "Exact Fit", "Best Value", "Budget Friendly", "Stretch Goal"

    public TrekRecommendationDto() {
    }

    public TrekRecommendationDto(Trek trek, BigDecimal totalBaseCostForGroup, BigDecimal estimatedTotalWithEssentials,
                                BigDecimal remainingBudget, boolean withinBudget, int matchScore, String matchBadge) {
        this.trek = trek;
        this.totalBaseCostForGroup = totalBaseCostForGroup;
        this.estimatedTotalWithEssentials = estimatedTotalWithEssentials;
        this.remainingBudget = remainingBudget;
        this.withinBudget = withinBudget;
        this.matchScore = matchScore;
        this.matchBadge = matchBadge;
    }

    public Trek getTrek() {
        return trek;
    }

    public void setTrek(Trek trek) {
        this.trek = trek;
    }

    public BigDecimal getTotalBaseCostForGroup() {
        return totalBaseCostForGroup;
    }

    public void setTotalBaseCostForGroup(BigDecimal totalBaseCostForGroup) {
        this.totalBaseCostForGroup = totalBaseCostForGroup;
    }

    public BigDecimal getEstimatedTotalWithEssentials() {
        return estimatedTotalWithEssentials;
    }

    public void setEstimatedTotalWithEssentials(BigDecimal estimatedTotalWithEssentials) {
        this.estimatedTotalWithEssentials = estimatedTotalWithEssentials;
    }

    public BigDecimal getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(BigDecimal remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public boolean isWithinBudget() {
        return withinBudget;
    }

    public void setWithinBudget(boolean withinBudget) {
        this.withinBudget = withinBudget;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchBadge() {
        return matchBadge;
    }

    public void setMatchBadge(String matchBadge) {
        this.matchBadge = matchBadge;
    }
}

