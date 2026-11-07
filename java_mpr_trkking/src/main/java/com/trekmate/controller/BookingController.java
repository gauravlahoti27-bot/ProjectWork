package com.trekmate.controller;

import com.trekmate.dto.BookingRequestDto;
import com.trekmate.model.Booking;
import com.trekmate.model.User;
import com.trekmate.service.BookingService;
import com.trekmate.service.UserService;
import com.trekmate.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;
    private final WalletService walletService;

    @Autowired
    public BookingController(BookingService bookingService, UserService userService, WalletService walletService) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.walletService = walletService;
    }

    @PostMapping("/submit")
    public String submitBooking(@ModelAttribute BookingRequestDto request,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login?redirect=/plan/" + request.getTrekId();
        }

        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.createBooking(user, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking request submitted successfully! Booking Code: " + booking.getBookingCode());
            return "redirect:/bookings/confirmation/" + booking.getBookingCode();
        } catch (IllegalStateException ex) {
            // Check if it's an insufficient funds issue
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("showWalletTopupPrompt", true);
            return "redirect:/plan/" + request.getTrekId() + "?groupSize=" + request.getGroupSize()
                    + "&dateId=" + request.getTrekDateId()
                    + (request.getUserBudget() != null ? "&budget=" + request.getUserBudget() : "");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing booking: " + ex.getMessage());
            return "redirect:/plan/" + request.getTrekId();
        }
    }

    @GetMapping("/confirmation/{code}")
    public String bookingConfirmation(@PathVariable("code") String code, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<Booking> bookingOpt = bookingService.getBookingByCode(code);
        if (bookingOpt.isEmpty()) {
            return "redirect:/bookings/my";
        }

        Booking booking = bookingOpt.get();
        BigDecimal remainingWalletBalance = walletService.getBalance(userId);

        model.addAttribute("booking", booking);
        model.addAttribute("remainingWalletBalance", remainingWalletBalance);
        return "booking-confirmation";
    }

    @GetMapping("/my")
    public String myBookings(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login?redirect=/bookings/my";
        }

        List<Booking> upcomingBookings = bookingService.getUpcomingBookings(userId);
        List<Booking> pastBookings = bookingService.getPastOrInactiveBookings(userId);
        BigDecimal currentCredits = walletService.getBalance(userId);

        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("pastBookings", pastBookings);
        model.addAttribute("currentCredits", currentCredits);
        return "my-bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Booking cancelled = bookingService.cancelBookingByUser(id, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking " + cancelled.getBookingCode() + " has been cancelled. ₹" +
                            cancelled.getTotalCost() + " refunded back to your Virtual Wallet.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/bookings/my";
    }
}

