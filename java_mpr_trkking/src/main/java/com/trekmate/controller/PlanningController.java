package com.trekmate.controller;

import com.trekmate.dto.CostCalculationRequest;
import com.trekmate.dto.CostCalculationResponse;
import com.trekmate.model.Activity;
import com.trekmate.model.Trek;
import com.trekmate.model.TrekDate;
import com.trekmate.model.User;
import com.trekmate.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class PlanningController {

    private final TrekService trekService;
    private final ActivityService activityService;
    private final TrekDateService trekDateService;
    private final CostCalculatorService costCalculatorService;
    private final WalletService walletService;

    @Autowired
    public PlanningController(TrekService trekService,
                              ActivityService activityService,
                              TrekDateService trekDateService,
                              CostCalculatorService costCalculatorService,
                              WalletService walletService) {
        this.trekService = trekService;
        this.activityService = activityService;
        this.trekDateService = trekDateService;
        this.costCalculatorService = costCalculatorService;
        this.walletService = walletService;
    }

    @GetMapping("/plan/{trekId}")
    public String showPlanPage(@PathVariable("trekId") Long trekId,
                               @RequestParam(value = "dateId", required = false) Long dateId,
                               @RequestParam(value = "groupSize", defaultValue = "1") Integer groupSize,
                               @RequestParam(value = "budget", required = false) BigDecimal budget,
                               Model model, HttpSession session) {
        Optional<Trek> trekOpt = trekService.getTrekById(trekId);
        if (trekOpt.isEmpty()) {
            return "redirect:/treks";
        }
        Trek trek = trekOpt.get();
        List<TrekDate> availableDates = trekDateService.getAvailableDatesForTrek(trekId);
        List<Activity> addOnActivities = activityService.getAddOnActivities();
        List<Activity> facilities = activityService.getFacilities();

        User currentUser = (User) session.getAttribute("currentUser");
        BigDecimal walletBalance = currentUser != null ? walletService.getBalance(currentUser.getId()) : BigDecimal.ZERO;

        // Perform initial baseline calculation for groupSize
        CostCalculationRequest calcReq = new CostCalculationRequest();
        calcReq.setTrekId(trekId);
        calcReq.setGroupSize(groupSize);
        calcReq.setUserBudget(budget != null ? budget : BigDecimal.ZERO);
        CostCalculationResponse initialCalc = costCalculatorService.calculate(calcReq);

        model.addAttribute("trek", trek);
        model.addAttribute("availableDates", availableDates);
        model.addAttribute("selectedDateId", dateId);
        model.addAttribute("groupSize", groupSize);
        model.addAttribute("budget", budget != null ? budget : "");
        model.addAttribute("addOnActivities", addOnActivities);
        model.addAttribute("facilities", facilities);
        model.addAttribute("initialCalc", initialCalc);
        model.addAttribute("walletBalance", walletBalance);

        return "plan";
    }
}

