package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDTO {
    
    private String promoId;
    private String code;
    private Double discount;
    private Double minValue;
    private LocalDateTime expireDate;
    private Integer status;
}
