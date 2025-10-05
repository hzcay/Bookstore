package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    
    private String orderItemId;
    private String orderId;
    private String bookId;
    private String bookTitle;
    private Integer quantity;
    private Double price;
    private Double subtotal;
}
