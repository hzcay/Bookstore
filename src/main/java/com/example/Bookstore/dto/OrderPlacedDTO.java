package com.example.Bookstore.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPlacedDTO {
    private String orderId;
    private String status;
    private double subtotal;
    private double discount;
    private double shippingFee;
    private double total;
    private int pointsUsed;
    private int pointsEarned;
}

