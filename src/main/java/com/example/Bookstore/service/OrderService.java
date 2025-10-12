package com.example.Bookstore.service;

import com.example.Bookstore.dto.OrderDTO;
import com.example.Bookstore.dto.OrderItemDTO;
import com.example.Bookstore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    
    Page<OrderDTO> getAllOrders(String customerId, Integer status, 
                               Integer paymentStatus, LocalDateTime dateFrom, 
                               LocalDateTime dateTo, Pageable pageable);
    
    Optional<OrderDTO> getOrderById(String orderId);
    
    OrderDTO createOrder(OrderDTO orderDTO);
    
    OrderDTO updateOrder(String orderId, OrderDTO orderDTO);
    
    void confirmOrder(String orderId);
    
    void cancelOrder(String orderId);
    
    void updatePaymentStatus(String orderId, Integer paymentStatus);
    
    OrderDTO addOrderItem(String orderId, OrderItemDTO orderItemDTO);
    
    void removeOrderItem(String orderId, String itemId);
    
    Double calculateOrderTotal(String orderId);
    
    List<OrderDTO> getOrdersByCustomer(String customerId);
    
    Double calculateRevenue(LocalDateTime fromDate, LocalDateTime toDate);
    
    Long countDeliveredOrders(LocalDateTime fromDate, LocalDateTime toDate);
}
