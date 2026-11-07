package com.trekmate.controller;

import com.trekmate.model.Trek;
import com.trekmate.model.TrekDate;
import com.trekmate.service.TrekDateService;
import com.trekmate.service.TrekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class TrekController {

    private final TrekService trekService;
    private final TrekDateService trekDateService;

    @Autowired
    public TrekController(TrekService trekService, TrekDateService trekDateService) {
        this.trekService = trekService;
        this.trekDateService = trekDateService;
    }

    @GetMapping("/treks")
    public String listTreks(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "difficulty", required = false) String difficulty,
                            Model model) {
        List<Trek> treks;
        if (search != null && !search.trim().isEmpty()) {
            treks = trekService.searchTreks(search.trim());
        } else {
            treks = trekService.getAllActiveTreks();
        }

        if (difficulty != null && !difficulty.trim().isEmpty() && !"ALL".equalsIgnoreCase(difficulty)) {
            treks = treks.stream()
                    .filter(t -> t.getDifficulty().equalsIgnoreCase(difficulty.trim()))
                    .toList();
        }

        model.addAttribute("treks", treks);
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedDifficulty", difficulty);
        return "treks";
    }

    @GetMapping("/treks/{id}")
    public String trekDetails(@PathVariable("id") Long id, Model model) {
        Optional<Trek> trekOpt = trekService.getTrekById(id);
        if (trekOpt.isEmpty()) {
            return "redirect:/treks";
        }
        Trek trek = trekOpt.get();
        List<TrekDate> availableDates = trekDateService.getAvailableDatesForTrek(id);

        model.addAttribute("trek", trek);
        model.addAttribute("availableDates", availableDates);
        return "trek-detail";
    }
}

