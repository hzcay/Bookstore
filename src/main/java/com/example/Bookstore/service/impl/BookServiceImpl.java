package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.BookDTO;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookServiceImpl implements BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Page<BookDTO> getAllBooks(Pageable pageable) {
        return bookRepository.findAllActive(pageable)
                .map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BookDTO> searchBooks(String title, String categoryId, String authorId, 
                                     String publisherId, Double minPrice, Double maxPrice, 
                                     Pageable pageable) {
        return bookRepository.searchBooks(title, categoryId, authorId, publisherId, 
                                        minPrice, maxPrice, pageable)
                .map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BookDTO> getBookById(String bookId) {
        return bookRepository.findByBookIdAndStatus(bookId, 1)
                .map(this::convertToDTO);
    }
    
    @Override
    public BookDTO createBook(BookDTO bookDTO) {
        Book book = convertToEntity(bookDTO);
        book = bookRepository.save(book);
        return convertToDTO(book);
    }
    
    @Override
    public BookDTO updateBook(String bookId, BookDTO bookDTO) {
        Book existingBook = bookRepository.findByBookIdAndStatus(bookId, 1)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        updateEntityFromDTO(existingBook, bookDTO);
        existingBook = bookRepository.save(existingBook);
        return convertToDTO(existingBook);
    }
    
    @Override
    public void deleteBook(String bookId) {
        Book book = bookRepository.findByBookIdAndStatus(bookId, 1)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        book.setStatus(0);
        bookRepository.save(book);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getLowStockBooks(Integer threshold) {
        return bookRepository.findByQuantityLessThan(threshold)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean checkStockAvailability(String bookId, Integer quantity) {
        return bookRepository.findByBookIdAndStatus(bookId, 1)
                .map(book -> book.getQuantity() >= quantity)
                .orElse(false);
    }
    
    @Override
    public void updateStock(String bookId, Integer quantityChange) {
        Book book = bookRepository.findByBookIdAndStatus(bookId, 1)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        int newQuantity = book.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock");
        }
        
        book.setQuantity(newQuantity);
        bookRepository.save(book);
    }
    
    private BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setBookId(book.getBookId());
        dto.setTitle(book.getTitle());
        dto.setImportPrice(book.getImportPrice());
        dto.setSalePrice(book.getSalePrice());
        dto.setQuantity(book.getQuantity());
        dto.setCreateAt(book.getCreateAt());
        dto.setUpdateAt(book.getUpdateAt());
        dto.setStatus(book.getStatus());
        
        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getCategoryId());
            dto.setCategoryName(book.getCategory().getName());
        }
        
        if (book.getAuthor() != null) {
            dto.setAuthorId(book.getAuthor().getAuthorId());
            dto.setAuthorName(book.getAuthor().getName());
        }
        
        if (book.getPublisher() != null) {
            dto.setPublisherId(book.getPublisher().getPublisherId());
            dto.setPublisherName(book.getPublisher().getName());
        }
        
        return dto;
    }
    
    private Book convertToEntity(BookDTO dto) {
        Book book = new Book();
        // Không set bookId - để JPA tự sinh UUID
        book.setTitle(dto.getTitle());
        book.setImportPrice(dto.getImportPrice());
        book.setSalePrice(dto.getSalePrice());
        book.setQuantity(dto.getQuantity());
        book.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return book;
    }
    
    private void updateEntityFromDTO(Book book, BookDTO dto) {
        if (dto.getTitle() != null) book.setTitle(dto.getTitle());
        if (dto.getImportPrice() != null) book.setImportPrice(dto.getImportPrice());
        if (dto.getSalePrice() != null) book.setSalePrice(dto.getSalePrice());
        if (dto.getQuantity() != null) book.setQuantity(dto.getQuantity());
        if (dto.getStatus() != null) book.setStatus(dto.getStatus());
    }
}
