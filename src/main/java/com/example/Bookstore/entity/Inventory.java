package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventoryID")
    private String inventoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookID")
    private Book book;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplierID")
    private Supplier supplier;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "import_price")
    private Double importPrice;
    
    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "import_date")
    private LocalDateTime importDate;
    
    @UpdateTimestamp
    @Column(name = "update_at")
    private LocalDateTime updateAt;
    
    @Column(name = "status")
    private Integer status = 0;
}
