package com.trekmate.service;

import com.trekmate.dto.BookingRequestDto;
import com.trekmate.model.*;
import com.trekmate.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TrekRepository trekRepository;
    private final TrekDateRepository trekDateRepository;
    private final ActivityRepository activityRepository;
    private final WalletService walletService;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          BookingItemRepository bookingItemRepository,
                          TrekRepository trekRepository,
                          TrekDateRepository trekDateRepository,
                          ActivityRepository activityRepository,
                          WalletService walletService) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.trekRepository = trekRepository;
        this.trekDateRepository = trekDateRepository;
        this.activityRepository = activityRepository;
        this.walletService = walletService;
    }

    @Transactional
    public Booking createBooking(User user, BookingRequestDto request) {
        Trek trek = trekRepository.findById(request.getTrekId())
                .orElseThrow(() -> new IllegalArgumentException("Trek not found with ID: " + request.getTrekId()));

        TrekDate trekDate = trekDateRepository.findById(request.getTrekDateId())
                .orElseThrow(() -> new IllegalArgumentException("Selected date not found with ID: " + request.getTrekDateId()));

        int groupSize = request.getGroupSize();
        if (!trekDate.hasCapacityFor(groupSize)) {
            throw new IllegalStateException(String.format(
                    "Only %d slots are currently available for this batch, but you requested %d.",
                    trekDate.getAvailableSlots(), groupSize));
        }

        BigDecimal groupSizeDec = BigDecimal.valueOf(groupSize);
        BigDecimal baseCost = trek.getBasePrice().multiply(groupSizeDec);

        BigDecimal activitiesCost = BigDecimal.ZERO;
        BigDecimal facilitiesCost = BigDecimal.ZERO;

        Booking booking = new Booking();
        String bookingCode = "TRK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        booking.setBookingCode(bookingCode);
        booking.setUser(user);
        booking.setTrek(trek);
        booking.setTrekDate(trekDate);
        booking.setGroupSize(groupSize);
        booking.setBaseCost(baseCost);
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setStatus("PENDING");
        booking.setPaymentStatus("PAID_VIA_WALLET");
        booking.setCreatedAt(LocalDateTime.now());

        // Process line items (Activities and Facilities)
        List<Long> activityIds = request.getSelectedActivityIds();
        if (activityIds != null && !activityIds.isEmpty()) {
            List<Activity> activities = activityRepository.findByIdIn(activityIds);
            for (Activity act : activities) {
                int qty = act.isPerPerson() ? groupSize : 1;
                BigDecimal itemSubtotal = act.getPrice().multiply(BigDecimal.valueOf(qty));

                if (act.isFacility()) {
                    facilitiesCost = facilitiesCost.add(itemSubtotal);
                } else {
                    activitiesCost = activitiesCost.add(itemSubtotal);
                }

                BookingItem item = new BookingItem(booking, act, act.getPrice(), qty, itemSubtotal);
                booking.addItem(item);
            }
        }

        booking.setActivitiesCost(activitiesCost);
        booking.setFacilitiesCost(facilitiesCost);
        BigDecimal totalCost = baseCost.add(activitiesCost).add(facilitiesCost);
        booking.setTotalCost(totalCost);

        // Deduct from Virtual Wallet
        walletService.deductCredits(user.getId(), totalCost, bookingCode,
                String.format("Payment for %s (%d trekkers)", trek.getTitle(), groupSize));

        // Save booking & line items
        Booking savedBooking = bookingRepository.save(booking);

        return savedBooking;
    }

    @Transactional
    public Booking approveBooking(Long bookingId, String hostNotes) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is not in PENDING status.");
        }

        // Reserve slots in TrekDate
        TrekDate trekDate = booking.getTrekDate();
        if (!trekDate.hasCapacityFor(booking.getGroupSize())) {
            throw new IllegalStateException("Cannot approve booking: slots capacity exceeded for this date.");
        }
        trekDate.bookSlots(booking.getGroupSize());
        trekDateRepository.save(trekDate);

        booking.setStatus("APPROVED");
        booking.setHostNotes(hostNotes != null ? hostNotes : "Approved by Trek Leader. Get ready for adventure!");
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking rejectBooking(Long bookingId, String rejectionReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only PENDING bookings can be rejected.");
        }

        booking.setStatus("REJECTED");
        booking.setHostNotes(rejectionReason != null && !rejectionReason.trim().isEmpty()
                ? rejectionReason : "Batch capacity full or unfavorable trail conditions.");

        // Refund credits back to virtual wallet immediately
        walletService.refundCredits(
                booking.getUser().getId(),
                booking.getTotalCost(),
                booking.getBookingCode(),
                "Refund for Rejected Booking: " + booking.getBookingCode() + " (" + booking.getHostNotes() + ")"
        );

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBookingByUser(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("You are not authorized to cancel this booking.");
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "REJECTED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is already inactive.");
        }

        // If previously approved, release slots
        if ("APPROVED".equalsIgnoreCase(booking.getStatus())) {
            TrekDate trekDate = booking.getTrekDate();
            trekDate.releaseSlots(booking.getGroupSize());
            trekDateRepository.save(trekDate);
        }

        booking.setStatus("CANCELLED");
        booking.setHostNotes("Cancelled by trekker.");

        // Refund full amount to virtual wallet
        walletService.refundCredits(
                userId,
                booking.getTotalCost(),
                booking.getBookingCode(),
                "Refund for Trekker Cancellation: " + booking.getBookingCode()
        );

        return bookingRepository.save(booking);
    }

    public List<Booking> getUpcomingBookings(Long userId) {
        return bookingRepository.findUpcomingBookingsByUser(userId);
    }

    public List<Booking> getPastOrInactiveBookings(Long userId) {
        return bookingRepository.findPastOrInactiveBookingsByUser(userId);
    }

    public List<Booking> getAllBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getAllBookingsForHost() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public Optional<Booking> getBookingByCode(String code) {
        return bookingRepository.findByBookingCode(code);
    }
}

