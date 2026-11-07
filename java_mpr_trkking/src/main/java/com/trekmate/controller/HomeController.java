package com.trekmate.controller;

import com.trekmate.model.Trek;
import com.trekmate.service.TrekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final TrekService trekService;

    @Autowired
    public HomeController(TrekService trekService) {
        this.trekService = trekService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Trek> treks = trekService.getAllActiveTreks();
        model.addAttribute("featuredTreks", treks.size() > 6 ? treks.subList(0, 6) : treks);
        model.addAttribute("allTreksCount", treks.size());
        return "index";
    }
}

