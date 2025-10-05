package com.example.Bookstore.dto;

import com.example.Bookstore.entity.Shipment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDTO {
    
    private String shipmentId;
    private String orderId;
    private String shipperId;
    private String shipperName;
    private String deliveryAddress;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private Shipment.ShipmentStatus status;
    private Double codAmount;
}
