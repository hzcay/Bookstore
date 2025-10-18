package com.example.Bookstore.dto;

import com.example.Bookstore.entity.Shipment;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDTO {

    private String shipmentId;
    private String orderId;
    private String shipperId;
    private String shipperName;
    private String customerName; 
    private String deliveryAddress;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private Shipment.ShipmentStatus status;
    private Double codAmount;
    private String paymentMethod;
}
