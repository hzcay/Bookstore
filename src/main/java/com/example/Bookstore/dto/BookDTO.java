package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    
    private String bookId; 
    private String title;
    private String categoryId;
    private String categoryName;
    private String authorId;
    private String authorName;
    private String publisherId;
    private String publisherName;
    private Double importPrice;
    private Double salePrice;
    private Integer quantity;
    private Integer stockQuantity; // Tồn kho thực tế từ Inventory
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Integer status;
    private String thumbnail;
}
