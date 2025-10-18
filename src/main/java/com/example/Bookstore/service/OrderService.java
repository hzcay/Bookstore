package com.example.Bookstore.service;

import com.example.Bookstore.dto.CheckoutRequest;
import com.example.Bookstore.dto.OrderDTO;
import com.example.Bookstore.dto.OrderItemDTO;
import com.example.Bookstore.dto.OrderPlacedDTO;
import com.example.Bookstore.dto.OrderTrackDTO;
import com.example.Bookstore.entity.Order;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    
    Page<OrderDTO> getAllOrders(Pageable pageable);
    
    Page<OrderDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable);
    
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
    
    OrderTrackDTO track(String orderId, String email, String phone);
    
    OrderPlacedDTO guestCheckout(CheckoutRequest req, HttpSession session);
}
