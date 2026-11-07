package com.trekmate.service;

import com.trekmate.model.TrekDate;
import com.trekmate.repository.TrekDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TrekDateService {

    private final TrekDateRepository trekDateRepository;

    @Autowired
    public TrekDateService(TrekDateRepository trekDateRepository) {
        this.trekDateRepository = trekDateRepository;
    }

    public List<TrekDate> getAvailableDatesForTrek(Long trekId) {
        return trekDateRepository.findAvailableFutureDates(trekId, LocalDate.now());
    }

    public List<TrekDate> getAllDatesForTrek(Long trekId) {
        return trekDateRepository.findByTrekIdOrderByStartDateAsc(trekId);
    }

    public List<TrekDate> getAllDates() {
        return trekDateRepository.findAllByOrderByStartDateDesc();
    }

    public Optional<TrekDate> getById(Long id) {
        return trekDateRepository.findById(id);
    }

    @Transactional
    public TrekDate save(TrekDate trekDate) {
        return trekDateRepository.save(trekDate);
    }

    @Transactional
    public void deleteDate(Long id) {
        trekDateRepository.deleteById(id);
    }
}

