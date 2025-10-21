package com.example.Bookstore.service;

import com.example.Bookstore.dto.BookDTO;
import com.example.Bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookService {
    
    Page<BookDTO> getAllBooks(Pageable pageable);
    
    Page<BookDTO> getAllBooksForAdmin(Pageable pageable);
    
    Page<BookDTO> searchBooks(String title, String categoryId, String authorId, 
                             String publisherId, Double minPrice, Double maxPrice, 
                             Pageable pageable);
    
    Page<BookDTO> searchBooksForAdmin(String title, String categoryId, String authorId, 
                                     String publisherId, Double minPrice, Double maxPrice, 
                                     Pageable pageable);
    
    Optional<BookDTO> getBookById(String bookId);
    
    BookDTO createBook(BookDTO bookDTO);
    
    BookDTO updateBook(String bookId, BookDTO bookDTO);
    
    void deleteBook(String bookId);
    
    List<BookDTO> getLowStockBooks(Integer threshold);
    
    boolean checkStockAvailability(String bookId, Integer quantity);
    
    void updateStock(String bookId, Integer quantityChange);
}
