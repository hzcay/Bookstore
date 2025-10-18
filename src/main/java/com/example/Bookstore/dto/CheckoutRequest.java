package com.example.Bookstore.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CheckoutRequest {
    private String fullName;
    private String email; 
    private String phone;
    private String shippingAddress;
    private String promoCode;
    private Integer usePoints;
}

