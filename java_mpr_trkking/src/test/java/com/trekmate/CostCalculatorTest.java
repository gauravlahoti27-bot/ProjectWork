package com.trekmate;

import com.trekmate.dto.CostCalculationRequest;
import com.trekmate.dto.CostCalculationResponse;
import com.trekmate.model.Activity;
import com.trekmate.model.Trek;
import com.trekmate.repository.ActivityRepository;
import com.trekmate.repository.TrekRepository;
import com.trekmate.service.CostCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class CostCalculatorTest {

    private TrekRepository trekRepository;
    private ActivityRepository activityRepository;
    private CostCalculatorService costCalculatorService;

    @BeforeEach
    void setUp() {
        trekRepository = Mockito.mock(TrekRepository.class);
        activityRepository = Mockito.mock(ActivityRepository.class);
        costCalculatorService = new CostCalculatorService(trekRepository, activityRepository);
    }

    @Test
    void testAccurateCostCalculationAndBudgetComparison() {
        // Setup Trek: Base price Rs 10,000 per person
        Trek trek = new Trek();
        trek.setId(1L);
        trek.setTitle("Kedarkantha Summit");
        trek.setBasePrice(new BigDecimal("10000.00"));
        when(trekRepository.findById(1L)).thenReturn(Optional.of(trek));

        // Setup Activity (Per Person): River Rafting Rs 2,000
        Activity rafting = new Activity("River Rafting", "ACTIVITY", "Rafting", new BigDecimal("2000.00"), "PER_PERSON", "fa-water");
        rafting.setId(10L);

        // Setup Facility (Per Group): Certified Lead Guide Rs 3,000
        Activity guide = new Activity("Wilderness Guide", "FACILITY", "Guide", new BigDecimal("3000.00"), "PER_GROUP", "fa-shield");
        guide.setId(20L);

        when(activityRepository.findByIdIn(List.of(10L, 20L))).thenReturn(List.of(rafting, guide));

        // Group Size: 2, Budget: Rs 30,000
        CostCalculationRequest request = new CostCalculationRequest();
        request.setTrekId(1L);
        request.setGroupSize(2);
        request.setUserBudget(new BigDecimal("30000.00"));
        request.setSelectedActivityIds(List.of(10L, 20L));

        CostCalculationResponse response = costCalculatorService.calculate(request);

        // Expected:
        // Base: 10,000 * 2 = 20,000
        // Activities: 2,000 * 2 = 4,000
        // Facilities: 3,000 * 1 = 3,000
        // Total: 20,000 + 4,000 + 3,000 = 27,000
        // Budget: 30,000 -> Within Budget! Savings: 3,000
        assertEquals(new BigDecimal("20000.00"), response.getBaseCost());
        assertEquals(new BigDecimal("40000.00").compareTo(response.getBaseCost()), 1);
        assertEquals(new BigDecimal("4000.00"), response.getActivitiesCost());
        assertEquals(new BigDecimal("3000.00"), response.getFacilitiesCost());
        assertEquals(new BigDecimal("27000.00"), response.getTotalCost());
        assertEquals(new BigDecimal("13500.00"), response.getPerPersonCost());
        assertTrue(response.isWithinBudget());
        assertEquals(new BigDecimal("3000.00"), response.getBudgetDiff());
    }

    @Test
    void testOverBudgetTriggerAndSuggestions() {
        Trek trek = new Trek();
        trek.setId(2L);
        trek.setTitle("Chadar River Expedition");
        trek.setBasePrice(new BigDecimal("25000.00"));
        when(trekRepository.findById(2L)).thenReturn(Optional.of(trek));

        when(activityRepository.findByIdIn(List.of())).thenReturn(List.of());

        // Group size: 2 -> Base Cost: Rs 50,000. User Budget: Rs 30,000 (Exceeded by Rs 20,000)
        CostCalculationRequest request = new CostCalculationRequest();
        request.setTrekId(2L);
        request.setGroupSize(2);
        request.setUserBudget(new BigDecimal("30000.00"));

        CostCalculationResponse response = costCalculatorService.calculate(request);

        assertFalse(response.isWithinBudget());
        assertEquals(new BigDecimal("-20000.00"), response.getBudgetDiff());
        assertFalse(response.getSuggestions().isEmpty());
    }
}

