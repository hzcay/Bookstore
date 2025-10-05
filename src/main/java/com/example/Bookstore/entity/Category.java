package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @Column(name = "categoryID")
    private String categoryId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "status")
    private Integer status = 1;
}
