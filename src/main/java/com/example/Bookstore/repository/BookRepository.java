package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String>, JpaSpecificationExecutor<Book> {
    
    @Query("SELECT b FROM Book b WHERE b.status = 1")
    Page<Book> findAllActive(Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.status = 1 AND " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:categoryId IS NULL OR b.category.categoryId = :categoryId) AND " +
           "(:authorId IS NULL OR b.author.authorId = :authorId) AND " +
           "(:publisherId IS NULL OR b.publisher.publisherId = :publisherId) AND " +
           "(:minPrice IS NULL OR b.salePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.salePrice <= :maxPrice)")
    Page<Book> searchBooks(
            @Param("title") String title,
            @Param("categoryId") String categoryId,
            @Param("authorId") String authorId,
            @Param("publisherId") String publisherId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);
    
    List<Book> findByQuantityLessThan(Integer quantity);
    
    List<Book> findByQuantityLessThanAndStatus(Integer quantity, Integer status);
    
    Optional<Book> findByBookIdAndStatus(String bookId, Integer status);
    
    // ===== Warehouse methods =====
    interface StockAgg {
        String getBookId();       // books.bookID
        String getTitle();        // books.title
        String getAuthorName();   // authors.name
        String getCategoryName(); // categories.name
        Integer getStock();       // books.quantity
        Double getPrice();        // books.sale_price
    }
    
    @Query(
        value = """
            SELECT 
                b.bookID        AS bookId,
                b.title         AS title,
                a.name          AS authorName,
                c.name          AS categoryName,
                COALESCE(b.quantity,0) AS stock,
                b.sale_price    AS price
            FROM books b
                LEFT JOIN authors    a ON a.authorID   = b.authorID
                LEFT JOIN categories c ON c.categoryID = b.categoryID
            WHERE (:kw IS NULL OR :kw = '' 
                     OR LOWER(b.title)  LIKE LOWER(CONCAT('%', :kw, '%'))
                     OR LOWER(b.bookID) LIKE LOWER(CONCAT('%', :kw, '%'))
                  )
                AND (:cat IS NULL OR :cat = '' OR b.categoryID = :cat)
            ORDER BY b.title ASC
            """,
        countQuery = """
            SELECT COUNT(*) 
            FROM books b
                LEFT JOIN categories c ON c.categoryID = b.categoryID
            WHERE (:kw IS NULL OR :kw = '' 
                     OR LOWER(b.title)  LIKE LOWER(CONCAT('%', :kw, '%'))
                     OR LOWER(b.bookID) LIKE LOWER(CONCAT('%', :kw, '%'))
                  )
                AND (:cat IS NULL OR :cat = '' OR b.categoryID = :cat)
            """,
        nativeQuery = true
    )
    Page<StockAgg> pageStockAgg(@Param("kw") String keyword,
                                @Param("cat") String categoryId,
                                Pageable pageable);
    
    @Query(value = """
        SELECT COALESCE(SUM(b.quantity),0)
        FROM books b
            LEFT JOIN categories c ON c.categoryID = b.categoryID
        WHERE (:kw IS NULL OR :kw = '' 
                 OR LOWER(b.title)  LIKE LOWER(CONCAT('%', :kw, '%'))
                 OR LOWER(b.bookID) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            AND (:cat IS NULL OR :cat = '' OR b.categoryID = :cat)
        """, nativeQuery = true)
    Long sumQuantityFiltered(@Param("kw") String keyword, @Param("cat") String categoryId);
    
    @Query(value = """
        SELECT COUNT(*)
        FROM books b
            LEFT JOIN categories c ON c.categoryID = b.categoryID
        WHERE (:kw IS NULL OR :kw = '' 
                 OR LOWER(b.title)  LIKE LOWER(CONCAT('%', :kw, '%'))
                 OR LOWER(b.bookID) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            AND (:cat IS NULL OR :cat = '' OR b.categoryID = :cat)
        """, nativeQuery = true)
    long countFiltered(@Param("kw") String keyword, @Param("cat") String categoryId);
}
