package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bookID")
    private String bookId;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryID")
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorID")
    private Author author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisherID")
    private Publisher publisher;
    
    @Column(name = "import_price")
    private Double importPrice;
    
    @Column(name = "sale_price")
    private Double salePrice;
    
    @Column(name = "quantity")
    private Integer quantity = 0;
    
    @CreationTimestamp
    @Column(name = "create_at")
    private LocalDateTime createAt;
    
    @UpdateTimestamp
    @Column(name = "update_at")
    private LocalDateTime updateAt;
    
    @Column(name = "status")
    private Integer status = 1;
}
