package com.example.Bookstore.service;

import com.example.Bookstore.dto.PromotionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PromotionService {
    Page<PromotionDTO> getAllPromotions(String search, Pageable pageable);
    Optional<PromotionDTO> getPromotionById(String id);
    Optional<PromotionDTO> getPromotionByCode(String code);
    PromotionDTO createPromotion(PromotionDTO promotionDTO);
    PromotionDTO updatePromotion(String id, PromotionDTO promotionDTO);
    void deletePromotion(String id);
    List<PromotionDTO> getActivePromotions();
    void checkAndExpirePromotions();
    List<PromotionDTO> listActive(Double subtotal);
    Optional<PromotionDTO> validate(String code, Double subtotal);
}

