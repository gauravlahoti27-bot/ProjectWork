package com.trekmate.service;

import com.trekmate.dto.RecommendationRequest;
import com.trekmate.dto.TrekRecommendationDto;
import com.trekmate.model.Trek;
import com.trekmate.repository.TrekRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RecommendationService {

    private final TrekRepository trekRepository;

    @Autowired
    public RecommendationService(TrekRepository trekRepository) {
        this.trekRepository = trekRepository;
    }

    public List<TrekRecommendationDto> getRecommendations(RecommendationRequest request) {
        List<Trek> activeTreks = trekRepository.findByActiveTrue();
        int groupSize = request.getGroupSize();
        BigDecimal groupSizeDec = BigDecimal.valueOf(groupSize);
        BigDecimal budget = request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) > 0
                ? request.getBudget()
                : new BigDecimal("50000.00");

        List<TrekRecommendationDto> results = new ArrayList<>();

        for (Trek trek : activeTreks) {
            // Apply difficulty preference filter if provided and not "ANY"
            if (request.getDifficulty() != null && !request.getDifficulty().equalsIgnoreCase("ANY")
                    && !request.getDifficulty().trim().isEmpty()) {
                if (!trek.getDifficulty().equalsIgnoreCase(request.getDifficulty().trim())) {
                    continue;
                }
            }

            // Apply destination/state filter if provided
            if (request.getDestination() != null && !request.getDestination().trim().isEmpty()) {
                String search = request.getDestination().trim().toLowerCase();
                boolean matches = trek.getDestination().toLowerCase().contains(search) ||
                        (trek.getState() != null && trek.getState().toLowerCase().contains(search)) ||
                        trek.getTitle().toLowerCase().contains(search);
                if (!matches) {
                    continue;
                }
            }

            // Apply max duration filter if provided
            if (request.getMaxDuration() != null && request.getMaxDuration() > 0) {
                if (trek.getDurationDays() > request.getMaxDuration()) {
                    continue;
                }
            }

            BigDecimal totalBaseCost = trek.getBasePrice().multiply(groupSizeDec);
            // Estimated buffer with basic food pack (₹1,500/person) + emergency kit (₹600/person)
            BigDecimal estimatedEssentials = new BigDecimal("2100.00").multiply(groupSizeDec);
            BigDecimal estimatedTotal = totalBaseCost.add(estimatedEssentials);

            BigDecimal remaining = budget.subtract(totalBaseCost);
            boolean withinBudget = remaining.compareTo(BigDecimal.ZERO) >= 0;

            int matchScore;
            String badge;

            if (withinBudget) {
                // Calculate percentage of budget used
                double ratio = totalBaseCost.doubleValue() / budget.doubleValue();
                if (ratio >= 0.70 && ratio <= 0.95) {
                    matchScore = 95;
                    badge = "Best Value";
                } else if (ratio < 0.70) {
                    matchScore = 85;
                    badge = "Budget Friendly";
                } else {
                    matchScore = 90;
                    badge = "Exact Fit";
                }
            } else {
                BigDecimal deficit = totalBaseCost.subtract(budget);
                double overPercent = deficit.doubleValue() / budget.doubleValue();
                if (overPercent <= 0.20) {
                    matchScore = 65;
                    badge = "Slight Stretch";
                } else {
                    matchScore = 40;
                    badge = "Premium Choice";
                }
            }

            results.add(new TrekRecommendationDto(
                    trek,
                    totalBaseCost,
                    estimatedTotal,
                    remaining,
                    withinBudget,
                    matchScore,
                    badge
            ));
        }

        // Sort by withinBudget first (true before false), then by matchScore descending, then by remaining budget
        results.sort(Comparator.comparing(TrekRecommendationDto::isWithinBudget).reversed()
                .thenComparing(Comparator.comparingInt(TrekRecommendationDto::getMatchScore).reversed())
                .thenComparing(TrekRecommendationDto::getTotalBaseCostForGroup));

        return results;
    }
}

