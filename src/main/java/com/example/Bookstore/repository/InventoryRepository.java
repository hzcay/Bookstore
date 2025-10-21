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
    
    // ===== Warehouse methods =====
    @Query("""
        SELECT i FROM Inventory i
        WHERE (:from IS NULL OR i.importDate >= :from)
        AND (:to IS NULL OR i.importDate < :to)
        ORDER BY i.importDate DESC
    """)
    Page<Inventory> findByDateRange(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  Pageable pageable);
    
    // Alternative method with native query for better PostgreSQL compatibility
    @Query(value = """
        SELECT * FROM inventory i
        WHERE (:#{#from == null} OR i.import_date >= :from)
        AND (:#{#to == null} OR i.import_date < :to)
        ORDER BY i.import_date DESC
        """, nativeQuery = true)
    Page<Inventory> findByDateRangeNative(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);
    
    // Tổng tồn hiện tại = sum(quantity nhập) - sum(quantity xuất)
    // Ở đây giả sử bản ghi Inventory.quantity >0 là nhập, <0 là xuất
    @Query(value = """
        SELECT b.bookID AS bookId, b.title AS title,
        COALESCE(SUM(i.quantity),0) AS stock
        FROM books b
        LEFT JOIN inventory i ON i.bookid = b.bookID
        WHERE (:kw IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR b.bookID LIKE CONCAT('%', :kw, '%'))
        GROUP BY b.bookID, b.title
        ORDER BY b.title ASC
        """, nativeQuery = true)
    Page<StockView> getCurrentStock(@Param("kw") String keyword, Pageable pageable);
    
    interface StockView {
        String getBookId();
        String getTitle();
        Long getStock();
    }

    // Simple date range methods to avoid IS NULL issues
    Page<Inventory> findByImportDateBefore(LocalDateTime to, Pageable pageable);
    Page<Inventory> findByImportDateAfter(LocalDateTime from, Pageable pageable);
    Page<Inventory> findByImportDateBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
