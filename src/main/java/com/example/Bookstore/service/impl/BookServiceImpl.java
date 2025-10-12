package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.BookDTO;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.entity.Category;
import com.example.Bookstore.entity.Author;
import com.example.Bookstore.entity.Publisher;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.CategoryRepository;
import com.example.Bookstore.repository.AuthorRepository;
import com.example.Bookstore.repository.PublisherRepository;
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
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @Autowired
    private PublisherRepository publisherRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Page<BookDTO> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable)
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
        return bookRepository.findById(bookId)
                .map(this::convertToDTO);
    }
    
    @Override
    public BookDTO createBook(BookDTO bookDTO) {
        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setImportPrice(bookDTO.getImportPrice());
        book.setSalePrice(bookDTO.getSalePrice());
        book.setQuantity(bookDTO.getQuantity() != null ? bookDTO.getQuantity() : 0);
        book.setStatus(1);
        
        if (bookDTO.getCategoryId() != null && !bookDTO.getCategoryId().isEmpty()) {
            Category category = categoryRepository.findById(bookDTO.getCategoryId()).orElse(null);
            book.setCategory(category);
        }
        
        if (bookDTO.getAuthorId() != null && !bookDTO.getAuthorId().isEmpty()) {
            Author author = authorRepository.findById(bookDTO.getAuthorId()).orElse(null);
            book.setAuthor(author);
        }
        
        if (bookDTO.getPublisherId() != null && !bookDTO.getPublisherId().isEmpty()) {
            Publisher publisher = publisherRepository.findById(bookDTO.getPublisherId()).orElse(null);
            book.setPublisher(publisher);
        }
        
        book = bookRepository.save(book);
        return convertToDTO(book);
    }
    
    @Override
    public BookDTO updateBook(String bookId, BookDTO bookDTO) {
        Book existingBook = bookRepository.findById(bookId)
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
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().isEmpty()) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            book.setCategory(category);
        } else {
            book.setCategory(null);
        }
        
        if (dto.getAuthorId() != null && !dto.getAuthorId().isEmpty()) {
            Author author = authorRepository.findById(dto.getAuthorId()).orElse(null);
            book.setAuthor(author);
        } else {
            book.setAuthor(null);
        }
        
        if (dto.getPublisherId() != null && !dto.getPublisherId().isEmpty()) {
            Publisher publisher = publisherRepository.findById(dto.getPublisherId()).orElse(null);
            book.setPublisher(publisher);
        } else {
            book.setPublisher(null);
        }
    }
}
