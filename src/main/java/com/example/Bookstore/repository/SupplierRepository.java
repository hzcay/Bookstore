package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {
    
    @Query("SELECT s FROM Supplier s WHERE s.status = 1 AND " +
           "(:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Supplier> searchSuppliers(@Param("q") String q, Pageable pageable);
    
    List<Supplier> findByStatus(Integer status);
    
    Optional<Supplier> findBySupplierIdAndStatus(String supplierId, Integer status);
    
    @Query("SELECT s FROM Supplier s WHERE s.debt > 0 AND s.status = 1")
    List<Supplier> findSuppliersWithDebt();
}
