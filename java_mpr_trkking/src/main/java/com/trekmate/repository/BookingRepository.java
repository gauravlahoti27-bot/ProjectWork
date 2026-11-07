package com.trekmate.repository;

import com.trekmate.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.trekDate.startDate >= CURRENT_DATE AND b.status IN ('PENDING', 'APPROVED') ORDER BY b.trekDate.startDate ASC")
    List<Booking> findUpcomingBookingsByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND (b.trekDate.startDate < CURRENT_DATE OR b.status IN ('REJECTED', 'CANCELLED')) ORDER BY b.createdAt DESC")
    List<Booking> findPastOrInactiveBookingsByUser(@Param("userId") Long userId);

    List<Booking> findByStatus(String status);
}

