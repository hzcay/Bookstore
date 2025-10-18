package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {
    
    Optional<Promotion> findByCode(String code);
    
    Optional<Promotion> findByCodeIgnoreCaseAndStatusAndExpireDateAfter(String code, Integer status, LocalDateTime now);
    
    List<Promotion> findByStatusAndExpireDateAfterOrderByExpireDateAsc(Integer status, LocalDateTime now);
    
    List<Promotion> findByStatus(Integer status);
    
    Page<Promotion> findByStatus(Integer status, Pageable pageable);
    
    @Query("SELECT p FROM Promotion p WHERE LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Promotion> findByCodeContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM Promotion p WHERE p.expireDate <= :now AND p.status = 1")
    List<Promotion> findExpiredPromotions(@Param("now") LocalDateTime now);
}
