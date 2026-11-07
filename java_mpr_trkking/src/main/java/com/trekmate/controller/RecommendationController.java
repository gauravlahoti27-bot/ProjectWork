package com.trekmate.controller;

import com.trekmate.dto.RecommendationRequest;
import com.trekmate.dto.TrekRecommendationDto;
import com.trekmate.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public String showRecommendationPage(Model model) {
        RecommendationRequest defaultReq = new RecommendationRequest();
        defaultReq.setBudget(new BigDecimal("30000.00"));
        defaultReq.setGroupSize(2);
        defaultReq.setDifficulty("ANY");

        List<TrekRecommendationDto> recommendations = recommendationService.getRecommendations(defaultReq);

        model.addAttribute("request", defaultReq);
        model.addAttribute("recommendations", recommendations);
        return "recommendations";
    }

    @PostMapping("/recommendations")
    public String processRecommendation(@ModelAttribute RecommendationRequest request, Model model) {
        List<TrekRecommendationDto> recommendations = recommendationService.getRecommendations(request);

        model.addAttribute("request", request);
        model.addAttribute("recommendations", recommendations);
        return "recommendations";
    }
}

