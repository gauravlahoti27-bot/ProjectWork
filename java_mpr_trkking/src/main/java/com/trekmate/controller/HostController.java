package com.trekmate.controller;

import com.trekmate.model.*;
import com.trekmate.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/host")
public class HostController {

    private final TrekService trekService;
    private final ActivityService activityService;
    private final TrekDateService trekDateService;
    private final BookingService bookingService;

    @Autowired
    public HostController(TrekService trekService,
                          ActivityService activityService,
                          TrekDateService trekDateService,
                          BookingService bookingService) {
        this.trekService = trekService;
        this.activityService = activityService;
        this.trekDateService = trekDateService;
        this.bookingService = bookingService;
    }

    private boolean isHost(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "HOST".equalsIgnoreCase(role);
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login?redirect=/host/dashboard";
        }

        List<Booking> allBookings = bookingService.getAllBookingsForHost();
        long pendingCount = allBookings.stream().filter(Booking::isPending).count();
        long approvedCount = allBookings.stream().filter(Booking::isApproved).count();
        List<Trek> treks = trekService.getAllTreksForAdmin();
        List<Activity> activities = activityService.getAllActivities();

        model.addAttribute("bookings", allBookings);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("treksCount", treks.size());
        model.addAttribute("activitiesCount", activities.size());
        return "host-dashboard";
    }

    // --- Trek Management ---
    @GetMapping("/treks")
    public String manageTreks(HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        model.addAttribute("treks", trekService.getAllTreksForAdmin());
        return "host-treks";
    }

    @GetMapping("/treks/add")
    public String showAddTrekForm(HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        model.addAttribute("trek", new Trek());
        model.addAttribute("isEdit", false);
        return "host-trek-form";
    }

    @GetMapping("/treks/edit/{id}")
    public String showEditTrekForm(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        Trek trek = trekService.getTrekById(id).orElse(null);
        if (trek == null) {
            return "redirect:/host/treks";
        }
        model.addAttribute("trek", trek);
        model.addAttribute("isEdit", true);
        return "host-trek-form";
    }

    @PostMapping("/treks/save")
    public String saveTrek(@ModelAttribute Trek trek, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        if (trek.getActive() == null) {
            trek.setActive(true);
        }
        trekService.saveTrek(trek);
        redirectAttributes.addFlashAttribute("successMessage", "Trek saved successfully!");
        return "redirect:/host/treks";
    }

    @PostMapping("/treks/delete/{id}")
    public String deleteTrek(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        trekService.deleteTrek(id);
        redirectAttributes.addFlashAttribute("successMessage", "Trek deactivated successfully.");
        return "redirect:/host/treks";
    }

    // --- Activity & Facility Management ---
    @GetMapping("/activities")
    public String manageActivities(HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        model.addAttribute("activities", activityService.getAllActivities());
        model.addAttribute("newActivity", new Activity());
        return "host-activities";
    }

    @PostMapping("/activities/save")
    public String saveActivity(@ModelAttribute Activity activity, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        activityService.saveActivity(activity);
        redirectAttributes.addFlashAttribute("successMessage", "Activity/Facility saved successfully!");
        return "redirect:/host/activities";
    }

    @PostMapping("/activities/toggle/{id}")
    public String toggleActivity(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        activityService.toggleActiveStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Activity status updated.");
        return "redirect:/host/activities";
    }

    // --- Batch Dates & Capacity Management ---
    @GetMapping("/dates")
    public String manageDates(HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        model.addAttribute("dates", trekDateService.getAllDates());
        model.addAttribute("treks", trekService.getAllActiveTreks());
        return "host-dates";
    }

    @PostMapping("/dates/add")
    public String addDate(@RequestParam("trekId") Long trekId,
                          @RequestParam("startDate") String startDateStr,
                          @RequestParam("endDate") String endDateStr,
                          @RequestParam("totalSlots") Integer totalSlots,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }

        try {
            Trek trek = trekService.getTrekById(trekId).orElseThrow();
            LocalDate start = LocalDate.parse(startDateStr);
            LocalDate end = LocalDate.parse(endDateStr);

            TrekDate td = new TrekDate(trek, start, end, totalSlots);
            trekDateService.save(td);
            redirectAttributes.addFlashAttribute("successMessage", "New batch date created successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add date: " + ex.getMessage());
        }

        return "redirect:/host/dates";
    }

    @PostMapping("/dates/delete/{id}")
    public String deleteDate(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        trekDateService.deleteDate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Batch date removed.");
        return "redirect:/host/dates";
    }

    // --- Booking Approval & Rejection ---
    @GetMapping("/bookings")
    public String manageBookings(HttpSession session, Model model) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        model.addAttribute("bookings", bookingService.getAllBookingsForHost());
        return "host-bookings";
    }

    @PostMapping("/bookings/{id}/approve")
    public String approveBooking(@PathVariable("id") Long id,
                                 @RequestParam(value = "hostNotes", required = false) String hostNotes,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        try {
            Booking approved = bookingService.approveBooking(id, hostNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking " + approved.getBookingCode() + " approved! Slots locked.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/host/bookings";
    }

    @PostMapping("/bookings/{id}/reject")
    public String rejectBooking(@PathVariable("id") Long id,
                                @RequestParam(value = "hostNotes", required = false) String hostNotes,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isHost(session)) {
            return "redirect:/login";
        }
        try {
            Booking rejected = bookingService.rejectBooking(id, hostNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking " + rejected.getBookingCode() + " rejected. Amount refunded to user wallet.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/host/bookings";
    }
}

