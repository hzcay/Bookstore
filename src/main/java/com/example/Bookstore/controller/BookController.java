package com.example.Bookstore.controller;

import com.example.Bookstore.dto.BookDTO;
import com.example.Bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/books")
@CrossOrigin(origins = "*")
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    @Value("${app.api-base:/api/v1}")
    private String apiBase;
    
    @GetMapping
    public ResponseEntity<Page<BookDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BookDTO> books = bookService.getAllBooks(pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/admin")
    public ResponseEntity<Page<BookDTO>> getAllBooksForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BookDTO> books = bookService.getAllBooksForAdmin(pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<BookDTO>> searchBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String authorId,
            @RequestParam(required = false) String publisherId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BookDTO> books = bookService.searchBooks(
                title != null ? title : q, categoryId, authorId, publisherId, 
                minPrice, maxPrice, pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable String id) {
        Optional<BookDTO> book = bookService.getBookById(id);
        return book.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {
        BookDTO createdBook = bookService.createBook(bookDTO);
        return ResponseEntity.ok(createdBook);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable String id, 
                                             @RequestBody BookDTO bookDTO) {
        try {
            BookDTO updatedBook = bookService.updateBook(id, bookDTO);
            return ResponseEntity.ok(updatedBook);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable String id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<BookDTO>> getLowStockBooks(
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<BookDTO> books = bookService.getLowStockBooks(threshold);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/{id}/stock-check")
    public ResponseEntity<Boolean> checkStockAvailability(
            @PathVariable String id, 
            @RequestParam Integer quantity) {
        boolean available = bookService.checkStockAvailability(id, quantity);
        return ResponseEntity.ok(available);
    }

    @GetMapping({ "/homePage" }) // <- tên mới
    public String homePage(Model model, HttpServletRequest req) {
        model.addAttribute("uri", "/homePage"); // để highlight menu
        model.addAttribute("apiBase", apiBase);
        return "index"; // vẫn dùng templates/index.html để render danh sách
    }

    // ========== Home Page /List ==========
    @GetMapping({ "/browse", "/books" })
    public String browse(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI()); // để highlight menu
        model.addAttribute("apiBase", apiBase); // để layout -> window.API_BASE
        return "index"; // templates/index.html
    }

    // ========== Product Detail ==========
    @GetMapping("/book/{id}")
    public String bookDetail(@PathVariable String id, Model model, HttpServletRequest req) {
        model.addAttribute("uri", "/homePage"); // vẫn highlight Browse
        model.addAttribute("apiBase", apiBase);
        model.addAttribute("bookId", id); // nếu cần dùng trong JS
        return "book"; // templates/book.html (đã tạo theo hướng dẫn)
    }

    // ========== Cart ==========
    @GetMapping("/cart")
    public String cart(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("apiBase", apiBase);
        return "cart";
    }

    // ========== Checkout ==========
    @GetMapping("/checkout")
    public String checkout(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("apiBase", apiBase);
        return "checkout";
    }

}
