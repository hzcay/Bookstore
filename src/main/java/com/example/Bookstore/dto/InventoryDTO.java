package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {
    
    private String inventoryId;
    private String bookId;
    private String bookTitle;
    private String supplierId;
    private String supplierName;
    private Integer quantity;
    private LocalDateTime importDate;
    private LocalDateTime updateAt;
    private Integer status;
}
