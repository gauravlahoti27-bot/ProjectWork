package com.trekmate.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CostCalculationRequest {
    private Long trekId;
    private Integer groupSize = 1;
    private BigDecimal userBudget;
    private List<Long> selectedActivityIds = new ArrayList<>();

    public CostCalculationRequest() {
    }

    public Long getTrekId() {
        return trekId;
    }

    public void setTrekId(Long trekId) {
        this.trekId = trekId;
    }

    public Integer getGroupSize() {
        return groupSize != null && groupSize > 0 ? groupSize : 1;
    }

    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }

    public BigDecimal getUserBudget() {
        return userBudget;
    }

    public void setUserBudget(BigDecimal userBudget) {
        this.userBudget = userBudget;
    }

    public List<Long> getSelectedActivityIds() {
        return selectedActivityIds != null ? selectedActivityIds : new ArrayList<>();
    }

    public void setSelectedActivityIds(List<Long> selectedActivityIds) {
        this.selectedActivityIds = selectedActivityIds;
    }
}

