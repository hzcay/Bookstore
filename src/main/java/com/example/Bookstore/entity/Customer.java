package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @Column(name = "customerID")
    private String customerId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "phone", unique = true)
    private String phone;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "points")
    private Integer points = 0;
    
    @Column(name = "status")
    private Integer status = 1;
}
