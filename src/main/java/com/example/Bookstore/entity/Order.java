package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "orderID")
    private String orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerID")
    private Customer customer;
    
    @Column(name = "total")
    private Double total = 0.0;
    
    @Column(name = "discount")
    private Double discount = 0.0;
    
    @Column(name = "shipping_fee")
    private Double shippingFee = 0.0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    @Column(name = "payment_status")
    private Integer paymentStatus = 0;
    
    @CreationTimestamp
    @Column(name = "create_at")
    private LocalDateTime createAt;
    
    @UpdateTimestamp
    @Column(name = "update_at")
    private LocalDateTime updateAt;
    
    @Column(name = "status")
    private Integer status = 0;
    
    @Column(name = "shipping_address")
    private String shippingAddress;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
    
    public enum PaymentMethod {
        COD, ONLINE
    }
    
}
