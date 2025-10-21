package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "paymentid")
    private String paymentId;
    
    @ManyToOne
    @JoinColumn(name = "supplierID", nullable = false)
    private Supplier supplier;
    
    @Column(name = "payment_amount", nullable = false)
    private Double paymentAmount;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "note", length = 200)
    private String note;
    
    @Column(name = "remaining_debt")
    private Double remainingDebt;
    
    @Column(name = "employee_name", length = 100)
    private String employeeName;
    
    @Column(name = "status")
    private Integer status = 1;
}
