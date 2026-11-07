package com.trekmate.repository;

import com.trekmate.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByActiveTrue();
    List<Activity> findByCategoryAndActiveTrue(String category);
    List<Activity> findByCategory(String category);
    List<Activity> findByIdIn(List<Long> ids);
}

