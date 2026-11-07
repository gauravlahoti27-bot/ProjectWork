package com.trekmate.service;

import com.trekmate.model.Trek;
import com.trekmate.repository.TrekRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class TrekService {

    private final TrekRepository trekRepository;

    @Autowired
    public TrekService(TrekRepository trekRepository) {
        this.trekRepository = trekRepository;
    }

    public List<Trek> getAllActiveTreks() {
        return trekRepository.findByActiveTrue();
    }

    public List<Trek> getAllTreksForAdmin() {
        return trekRepository.findAll();
    }

    public List<Trek> searchTreks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllActiveTreks();
        }
        return trekRepository.searchTreks(query.trim());
    }

    public Optional<Trek> getTrekById(Long id) {
        return trekRepository.findById(id);
    }

    @Transactional
    public Trek saveTrek(Trek trek) {
        return trekRepository.save(trek);
    }

    @Transactional
    public void deleteTrek(Long id) {
        trekRepository.findById(id).ifPresent(t -> {
            t.setActive(false);
            trekRepository.save(t);
        });
    }
}

