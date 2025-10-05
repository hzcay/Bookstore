package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    
    @Query("SELECT b FROM Book b WHERE b.status = 1")
    Page<Book> findAllActive(Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.status = 1 AND " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:categoryId IS NULL OR b.category.categoryId = :categoryId) AND " +
           "(:authorId IS NULL OR b.author.authorId = :authorId) AND " +
           "(:publisherId IS NULL OR b.publisher.publisherId = :publisherId) AND " +
           "(:minPrice IS NULL OR b.salePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.salePrice <= :maxPrice)")
    Page<Book> searchBooks(@Param("title") String title,
                          @Param("categoryId") String categoryId,
                          @Param("authorId") String authorId,
                          @Param("publisherId") String publisherId,
                          @Param("minPrice") Double minPrice,
                          @Param("maxPrice") Double maxPrice,
                          Pageable pageable);
    
    List<Book> findByQuantityLessThan(Integer quantity);
    
    Optional<Book> findByBookIdAndStatus(String bookId, Integer status);
}
