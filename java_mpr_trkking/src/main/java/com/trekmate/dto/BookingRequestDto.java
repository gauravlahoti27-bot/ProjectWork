package com.trekmate.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BookingRequestDto {
    private Long trekId;
    private Long trekDateId;
    private Integer groupSize = 1;
    private BigDecimal userBudget;
    private List<Long> selectedActivityIds = new ArrayList<>();
    private String specialRequests;

    public BookingRequestDto() {
    }

    public Long getTrekId() {
        return trekId;
    }

    public void setTrekId(Long trekId) {
        this.trekId = trekId;
    }

    public Long getTrekDateId() {
        return trekDateId;
    }

    public void setTrekDateId(Long trekDateId) {
        this.trekDateId = trekDateId;
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

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }
}

