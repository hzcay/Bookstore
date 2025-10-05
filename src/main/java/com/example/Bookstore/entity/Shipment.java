package com.example.Bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    
    @Id
    @Column(name = "shipmentID")
    private String shipmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderID")
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipperID")
    private Employee shipper;
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;
    
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShipmentStatus status = ShipmentStatus.PICKING;
    
    @Column(name = "cod_amount")
    private Double codAmount = 0.0;
    
    public enum ShipmentStatus {
        PICKING, OUT_FOR_DELIVERY, DELIVERED, FAILED
    }
}
