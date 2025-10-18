package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.PromotionDTO;
import com.example.Bookstore.entity.Promotion;
import com.example.Bookstore.repository.PromotionRepository;
import com.example.Bookstore.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public Page<PromotionDTO> getAllPromotions(String search, Pageable pageable) {
        Page<Promotion> promotions;
        if (search != null && !search.trim().isEmpty()) {
            promotions = promotionRepository.findByCodeContainingIgnoreCase(search, pageable);
        } else {
            promotions = promotionRepository.findAll(pageable);
        }
        return promotions.map(this::convertToDTO);
    }

    @Override
    public Optional<PromotionDTO> getPromotionById(String id) {
        return promotionRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public Optional<PromotionDTO> getPromotionByCode(String code) {
        return promotionRepository.findByCode(code).map(this::convertToDTO);
    }

    @Override
    public PromotionDTO createPromotion(PromotionDTO promotionDTO) {
        Promotion promotion = new Promotion();
        promotion.setCode(promotionDTO.getCode());
        promotion.setDiscount(promotionDTO.getDiscount());
        promotion.setMinValue(promotionDTO.getMinValue());
        promotion.setExpireDate(promotionDTO.getExpireDate());
        
        if (promotionDTO.getExpireDate() != null && promotionDTO.getExpireDate().isBefore(LocalDateTime.now())) {
            promotion.setStatus(0);
        } else {
            promotion.setStatus(1);
        }
        
        Promotion saved = promotionRepository.save(promotion);
        return convertToDTO(saved);
    }

    @Override
    public PromotionDTO updatePromotion(String id, PromotionDTO promotionDTO) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Promotion not found"));
        
        promotion.setCode(promotionDTO.getCode());
        promotion.setDiscount(promotionDTO.getDiscount());
        promotion.setMinValue(promotionDTO.getMinValue());
        promotion.setExpireDate(promotionDTO.getExpireDate());
        
        if (promotionDTO.getExpireDate() != null && promotionDTO.getExpireDate().isBefore(LocalDateTime.now())) {
            promotion.setStatus(0);
        } else if (promotionDTO.getStatus() != null) {
            promotion.setStatus(promotionDTO.getStatus());
        }
        
        Promotion updated = promotionRepository.save(promotion);
        return convertToDTO(updated);
    }

    @Override
    public void deletePromotion(String id) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Promotion not found"));
        promotion.setStatus(0);
        promotionRepository.save(promotion);
    }

    @Override
    public List<PromotionDTO> getActivePromotions() {
        return promotionRepository.findByStatus(1).stream()
            .filter(p -> p.getExpireDate() == null || p.getExpireDate().isAfter(LocalDateTime.now()))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    public void checkAndExpirePromotions() {
        List<Promotion> expiredPromotions = promotionRepository.findExpiredPromotions(LocalDateTime.now());
        for (Promotion promotion : expiredPromotions) {
            promotion.setStatus(0);
            promotionRepository.save(promotion);
        }
    }

    private PromotionDTO convertToDTO(Promotion promotion) {
        PromotionDTO dto = new PromotionDTO();
        dto.setPromoId(promotion.getPromoId());
        dto.setCode(promotion.getCode());
        dto.setDiscount(promotion.getDiscount());
        dto.setMinValue(promotion.getMinValue());
        dto.setExpireDate(promotion.getExpireDate());
        dto.setStatus(promotion.getStatus());
        return dto;
    }
}

