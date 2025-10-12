package com.example.Bookstore.dto;

import com.example.Bookstore.entity.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    
    private String orderId;
    private String customerId;
    private String customerName;
    private Double total;
    private Double discount;
    private Double shippingFee;
    private Order.PaymentMethod paymentMethod;
    private Integer paymentStatus;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Integer status;
    private String shippingAddress;
    private List<OrderItemDTO> orderItems;
}
