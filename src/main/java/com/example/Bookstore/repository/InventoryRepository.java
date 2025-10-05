package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Inventory;
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
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    
    @Query("SELECT i FROM Inventory i WHERE " +
           "(:supplierId IS NULL OR i.supplier.supplierId = :supplierId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:dateFrom IS NULL OR i.importDate >= :dateFrom) AND " +
           "(:dateTo IS NULL OR i.importDate <= :dateTo)")
    Page<Inventory> searchInventory(@Param("supplierId") String supplierId,
                                   @Param("status") Integer status,
                                   @Param("dateFrom") LocalDateTime dateFrom,
                                   @Param("dateTo") LocalDateTime dateTo,
                                   Pageable pageable);
    
    List<Inventory> findByBookBookIdAndStatus(String bookId, Integer status);
    
    Optional<Inventory> findByInventoryIdAndStatus(String inventoryId, Integer status);
    
    @Query("SELECT i FROM Inventory i WHERE i.book.bookId = :bookId AND i.status = 1")
    List<Inventory> findActiveByBookId(@Param("bookId") String bookId);
}
