package com.trekmate.controller;

import com.trekmate.dto.CostCalculationRequest;
import com.trekmate.dto.CostCalculationResponse;
import com.trekmate.service.CostCalculatorService;
import com.trekmate.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CostCalculatorService costCalculatorService;
    private final WalletService walletService;

    @Autowired
    public ApiController(CostCalculatorService costCalculatorService, WalletService walletService) {
        this.costCalculatorService = costCalculatorService;
        this.walletService = walletService;
    }

    @PostMapping("/calculate-cost")
    public ResponseEntity<CostCalculationResponse> calculateCost(@RequestBody CostCalculationRequest request) {
        CostCalculationResponse response = costCalculatorService.calculate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet-status")
    public ResponseEntity<Map<String, Object>> getWalletStatus(@RequestParam("totalCost") BigDecimal totalCost,
                                                              HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> res = new HashMap<>();

        if (userId == null) {
            res.put("loggedIn", false);
            res.put("balance", BigDecimal.ZERO);
            res.put("sufficient", false);
            res.put("deficit", totalCost);
            return ResponseEntity.ok(res);
        }

        BigDecimal balance = walletService.getBalance(userId);
        boolean sufficient = balance.compareTo(totalCost) >= 0;
        BigDecimal deficit = sufficient ? BigDecimal.ZERO : totalCost.subtract(balance);

        res.put("loggedIn", true);
        res.put("balance", balance);
        res.put("sufficient", sufficient);
        res.put("deficit", deficit);
        return ResponseEntity.ok(res);
    }
}

