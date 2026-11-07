package com.trekmate.repository;

import com.trekmate.model.TrekDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrekDateRepository extends JpaRepository<TrekDate, Long> {
    List<TrekDate> findByTrekIdOrderByStartDateAsc(Long trekId);

    @Query("SELECT d FROM TrekDate d WHERE d.trek.id = :trekId AND d.startDate >= :currentDate AND d.status = 'OPEN' ORDER BY d.startDate ASC")
    List<TrekDate> findAvailableFutureDates(@Param("trekId") Long trekId, @Param("currentDate") LocalDate currentDate);

    List<TrekDate> findAllByOrderByStartDateDesc();
}

