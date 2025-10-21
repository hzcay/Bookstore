package com.example.Bookstore.repository;

import com.example.Bookstore.entity.PaymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, String> {
    
    @Query("SELECT p FROM PaymentHistory p WHERE p.supplier.supplierId = :supplierId ORDER BY p.paymentDate DESC")
    Page<PaymentHistory> findBySupplierId(@Param("supplierId") String supplierId, Pageable pageable);
    
    @Query("SELECT p FROM PaymentHistory p WHERE " +
           "(:from IS NULL OR p.paymentDate >= :from) AND " +
           "(:to IS NULL OR p.paymentDate <= :to) " +
           "ORDER BY p.paymentDate DESC")
    Page<PaymentHistory> findByDateRange(@Param("from") LocalDateTime from, 
                                         @Param("to") LocalDateTime to, 
                                         Pageable pageable);
    
    @Query("SELECT p FROM PaymentHistory p ORDER BY p.paymentDate DESC")
    List<PaymentHistory> findAllForExport();
    
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM PaymentHistory p WHERE p.supplier.supplierId = :supplierId")
    Double getTotalPaidBySupplierId(@Param("supplierId") String supplierId);
}
