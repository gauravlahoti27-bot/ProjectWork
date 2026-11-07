package com.trekmate.repository;

import com.trekmate.model.Trek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TrekRepository extends JpaRepository<Trek, Long> {
    List<Trek> findByActiveTrue();

    @Query("SELECT t FROM Trek t WHERE t.active = true AND (:destination IS NULL OR LOWER(t.destination) LIKE LOWER(CONCAT('%', :destination, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :destination, '%')))")
    List<Trek> searchTreks(@Param("destination") String destination);

    @Query("SELECT t FROM Trek t WHERE t.active = true AND (:difficulty IS NULL OR LOWER(t.difficulty) = LOWER(:difficulty)) AND (:maxPrice IS NULL OR t.basePrice <= :maxPrice)")
    List<Trek> findByDifficultyAndMaxPrice(@Param("difficulty") String difficulty, @Param("maxPrice") BigDecimal maxPrice);

    List<Trek> findByActiveTrueOrderByBasePriceAsc();
}

