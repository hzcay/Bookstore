package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    
    @Id
    @Column(name = "promoID")
    private String promoId;
    
    @Column(name = "code", unique = true)
    private String code;
    
    @Column(name = "discount")
    private Double discount;
    
    @Column(name = "min_value")
    private Double minValue;
    
    @Column(name = "expire_date")
    private LocalDateTime expireDate;
    
    @Column(name = "status")
    private Integer status = 1;
}
