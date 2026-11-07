package com.trekmate.controller;

import com.trekmate.model.User;
import com.trekmate.model.WalletTransaction;
import com.trekmate.service.UserService;
import com.trekmate.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    @Autowired
    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping("/wallet")
    public String showWallet(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login?redirect=/wallet";
        }

        BigDecimal balance = walletService.getBalance(userId);
        List<WalletTransaction> transactions = walletService.getTransactionHistory(userId);
        User user = userService.findById(userId).orElse(null);

        model.addAttribute("balance", balance);
        model.addAttribute("transactions", transactions);
        model.addAttribute("user", user);
        return "wallet";
    }

    @PostMapping("/wallet/topup")
    public String topupCredits(@RequestParam("amount") BigDecimal amount,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login?redirect=/wallet";
        }

        try {
            walletService.allocateDemoCredits(userId, amount, reason != null ? reason : "Demo Credit Recharge");
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Successfully credited ₹%,.2f to your Virtual Wallet!", amount));
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/wallet";
    }
}

