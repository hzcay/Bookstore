package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {
    
    List<Promotion> findByStatus(Integer status);
    
    Optional<Promotion> findByCodeAndStatus(String code, Integer status);
    
    @Query("SELECT p FROM Promotion p WHERE p.status = 1 AND p.expireDate > :currentDate")
    List<Promotion> findActivePromotions(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.code = :code AND p.status = 1 AND p.expireDate > :currentDate")
    Optional<Promotion> findValidPromotion(@Param("code") String code, 
                                          @Param("currentDate") LocalDateTime currentDate);
}
