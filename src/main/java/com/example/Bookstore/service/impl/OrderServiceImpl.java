package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.OrderDTO;
import com.example.Bookstore.dto.OrderItemDTO;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.repository.OrderRepository;
import com.example.Bookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(String customerId, Integer status, 
                                       Integer paymentStatus, LocalDateTime dateFrom, 
                                       LocalDateTime dateTo, Pageable pageable) {
        return orderRepository.searchOrders(customerId, status, paymentStatus, 
                                          dateFrom, dateTo, pageable)
                .map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDTO> getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDTO);
    }
    
    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        Order order = convertToEntity(orderDTO);
        order = orderRepository.save(order);
        return convertToDTO(order);
    }
    
    @Override
    public OrderDTO updateOrder(String orderId, OrderDTO orderDTO) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        updateEntityFromDTO(existingOrder, orderDTO);
        existingOrder = orderRepository.save(existingOrder);
        return convertToDTO(existingOrder);
    }
    
    @Override
    public void confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == 0) {
            order.setStatus(1);
            orderRepository.save(order);
        } else {
            throw new RuntimeException("Order cannot be confirmed in current status");
        }
    }
    
    @Override
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == 0) {
            order.setStatus(0);
            orderRepository.save(order);
        } else {
            throw new RuntimeException("Order cannot be canceled in current status");
        }
    }
    
    @Override
    public void updatePaymentStatus(String orderId, Integer paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }
    
    @Override
    public OrderDTO addOrderItem(String orderId, OrderItemDTO orderItemDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != 0) {
            throw new RuntimeException("Cannot modify order items after confirmation");
        }
        
        return convertToDTO(order);
    }
    
    @Override
    public void removeOrderItem(String orderId, String itemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != 0) {
            throw new RuntimeException("Cannot modify order items after confirmation");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double calculateOrderTotal(String orderId) {
        return orderRepository.findById(orderId)
                .map(order -> order.getTotal() - order.getDiscount() + order.getShippingFee())
                .orElse(0.0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerCustomerIdAndStatus(customerId, 1)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double calculateRevenue(LocalDateTime fromDate, LocalDateTime toDate) {
        return orderRepository.calculateRevenue(fromDate, toDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long countDeliveredOrders(LocalDateTime fromDate, LocalDateTime toDate) {
        return orderRepository.countDeliveredOrders(fromDate, toDate);
    }
    
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setTotal(order.getTotal());
        dto.setDiscount(order.getDiscount());
        dto.setShippingFee(order.getShippingFee());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setCreateAt(order.getCreateAt());
        dto.setUpdateAt(order.getUpdateAt());
        dto.setStatus(order.getStatus());
        dto.setShippingAddress(order.getShippingAddress());
        
        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getCustomerId());
            dto.setCustomerName(order.getCustomer().getName());
        }
        
        if (order.getOrderItems() != null) {
            dto.setOrderItems(order.getOrderItems().stream()
                    .map(this::convertOrderItemToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private OrderItemDTO convertOrderItemToDTO(com.example.Bookstore.entity.OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(orderItem.getOrderItemId());
        dto.setOrderId(orderItem.getOrder().getOrderId());
        dto.setBookId(orderItem.getBook().getBookId());
        dto.setBookTitle(orderItem.getBook().getTitle());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setSubtotal(orderItem.getQuantity() * orderItem.getPrice());
        return dto;
    }
    
    private Order convertToEntity(OrderDTO dto) {
        Order order = new Order();
        order.setOrderId(dto.getOrderId());
        order.setTotal(dto.getTotal());
        order.setDiscount(dto.getDiscount());
        order.setShippingFee(dto.getShippingFee());
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setPaymentStatus(dto.getPaymentStatus());
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        order.setShippingAddress(dto.getShippingAddress());
        return order;
    }
    
    private void updateEntityFromDTO(Order order, OrderDTO dto) {
        if (dto.getTotal() != null) order.setTotal(dto.getTotal());
        if (dto.getDiscount() != null) order.setDiscount(dto.getDiscount());
        if (dto.getShippingFee() != null) order.setShippingFee(dto.getShippingFee());
        if (dto.getPaymentMethod() != null) order.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getPaymentStatus() != null) order.setPaymentStatus(dto.getPaymentStatus());
        if (dto.getStatus() != null) order.setStatus(dto.getStatus());
        if (dto.getShippingAddress() != null) order.setShippingAddress(dto.getShippingAddress());
    }
}