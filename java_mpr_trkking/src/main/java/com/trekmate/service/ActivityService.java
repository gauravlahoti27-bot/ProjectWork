package com.trekmate.service;

import com.trekmate.model.Activity;
import com.trekmate.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Autowired
    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public List<Activity> getAllActiveActivities() {
        return activityRepository.findByActiveTrue();
    }

    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    public List<Activity> getAddOnActivities() {
        return activityRepository.findByCategoryAndActiveTrue("ACTIVITY");
    }

    public List<Activity> getFacilities() {
        return activityRepository.findByCategoryAndActiveTrue("FACILITY");
    }

    public List<Activity> getActivitiesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return activityRepository.findByIdIn(ids);
    }

    public Optional<Activity> getById(Long id) {
        return activityRepository.findById(id);
    }

    @Transactional
    public Activity saveActivity(Activity activity) {
        return activityRepository.save(activity);
    }

    @Transactional
    public void toggleActiveStatus(Long id) {
        activityRepository.findById(id).ifPresent(act -> {
            act.setActive(!Boolean.TRUE.equals(act.getActive()));
            activityRepository.save(act);
        });
    }
}

