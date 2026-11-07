package com.trekmate.service;

import com.trekmate.dto.CostCalculationRequest;
import com.trekmate.dto.CostCalculationResponse;
import com.trekmate.model.Activity;
import com.trekmate.model.Trek;
import com.trekmate.repository.ActivityRepository;
import com.trekmate.repository.TrekRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementing object-oriented business logic for trip budgeting,
 * cost breakdown computation, budget comparison, and cost-reduction suggestions.
 */
@Service
public class CostCalculatorService {

    private final TrekRepository trekRepository;
    private final ActivityRepository activityRepository;

    @Autowired
    public CostCalculatorService(TrekRepository trekRepository, ActivityRepository activityRepository) {
        this.trekRepository = trekRepository;
        this.activityRepository = activityRepository;
    }

    public CostCalculationResponse calculate(CostCalculationRequest request) {
        CostCalculationResponse response = new CostCalculationResponse();

        Trek trek = trekRepository.findById(request.getTrekId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid trek ID: " + request.getTrekId()));

        int groupSize = request.getGroupSize();
        BigDecimal groupSizeDec = BigDecimal.valueOf(groupSize);

        // 1. Base Cost
        BigDecimal baseCost = trek.getBasePrice().multiply(groupSizeDec);
        response.setBaseCost(baseCost);

        // 2. Add-on Activities & Facilities Costs
        BigDecimal activitiesCost = BigDecimal.ZERO;
        BigDecimal facilitiesCost = BigDecimal.ZERO;

        List<Activity> selectedActivities = new ArrayList<>();
        if (!request.getSelectedActivityIds().isEmpty()) {
            selectedActivities = activityRepository.findByIdIn(request.getSelectedActivityIds());
            for (Activity item : selectedActivities) {
                BigDecimal itemCost;
                if (item.isPerPerson()) {
                    itemCost = item.getPrice().multiply(groupSizeDec);
                } else {
                    itemCost = item.getPrice();
                }

                if (item.isFacility()) {
                    facilitiesCost = facilitiesCost.add(itemCost);
                } else {
                    activitiesCost = activitiesCost.add(itemCost);
                }
            }
        }

        response.setActivitiesCost(activitiesCost);
        response.setFacilitiesCost(facilitiesCost);

        // 3. Total Trip Cost
        BigDecimal totalCost = baseCost.add(activitiesCost).add(facilitiesCost);
        response.setTotalCost(totalCost);
        response.setPerPersonCost(totalCost.divide(groupSizeDec, 2, RoundingMode.HALF_UP));

        // 4. Budget Comparison
        BigDecimal userBudget = request.getUserBudget() != null ? request.getUserBudget() : BigDecimal.ZERO;
        response.setUserBudget(userBudget);

        if (userBudget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal budgetDiff = userBudget.subtract(totalCost);
            response.setBudgetDiff(budgetDiff);
            boolean isWithin = budgetDiff.compareTo(BigDecimal.ZERO) >= 0;
            response.setWithinBudget(isWithin);

            List<String> suggestions = new ArrayList<>();

            if (!isWithin) {
                BigDecimal deficit = budgetDiff.abs();
                suggestions.add(String.format("You are exceeding your budget by ₹%,.2f for a group of %d.", deficit, groupSize));

                // Suggest deselecting high-cost add-on activities
                for (Activity item : selectedActivities) {
                    BigDecimal itemTotal = item.isPerPerson() ? item.getPrice().multiply(groupSizeDec) : item.getPrice();
                    if (!item.isFacility()) {
                        suggestions.add(String.format("Deselect optional activity '%s' to save ₹%,.2f.",
                                item.getName(), itemTotal));
                    } else if (!item.getName().toLowerCase().contains("emergency")) {
                        suggestions.add(String.format("Consider removing optional facility '%s' to save ₹%,.2f.",
                                item.getName(), itemTotal));
                    }
                }

                // Suggest alternative cheaper treks that fit the budget for this group size
                List<Trek> allTreks = trekRepository.findByActiveTrueOrderByBasePriceAsc();
                List<CostCalculationResponse.CheaperAlternativeDto> alternatives = new ArrayList<>();

                for (Trek alt : allTreks) {
                    if (!alt.getId().equals(trek.getId())) {
                        BigDecimal altTotalForGroup = alt.getBasePrice().multiply(groupSizeDec);
                        if (altTotalForGroup.compareTo(userBudget) <= 0) {
                            BigDecimal savings = baseCost.subtract(altTotalForGroup);
                            alternatives.add(new CostCalculationResponse.CheaperAlternativeDto(
                                    alt.getId(),
                                    alt.getTitle(),
                                    alt.getDestination(),
                                    alt.getDifficulty(),
                                    alt.getBasePrice(),
                                    altTotalForGroup,
                                    savings
                            ));
                        }
                    }
                }

                if (!alternatives.isEmpty()) {
                    suggestions.add(String.format("Found %d alternative treks that fit within your budget of ₹%,.2f!",
                            alternatives.size(), userBudget));
                    response.setAlternativeTreks(alternatives);
                } else {
                    suggestions.add("Tip: You can top-up your Virtual Wallet with demo credits to cover the difference!");
                }
            } else {
                suggestions.add(String.format("Great news! This customized trek is well within your budget with ₹%,.2f in savings.",
                        budgetDiff));
            }

            response.setSuggestions(suggestions);
        } else {
            response.setWithinBudget(true);
            response.setBudgetDiff(BigDecimal.ZERO);
        }

        return response;
    }
}

