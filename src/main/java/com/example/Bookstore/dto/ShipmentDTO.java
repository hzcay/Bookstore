package com.example.Bookstore.dto;

import com.example.Bookstore.entity.Order;
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
    private String orderCustomerName;
    private Double orderTotal;
    private Order.OrderStatus orderStatus;  // Trạng thái của Order
    private String shipperId;
    private String shipperName;
    private String customerName; // Để tương thích với shipper module
    private String deliveryAddress;
    private LocalDateTime createAt;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private Shipment.ShipmentStatus status;  // Trạng thái của Shipment
    private Double codAmount;
    private String paymentMethod;
}
