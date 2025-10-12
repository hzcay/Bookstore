package com.example.Bookstore.controller;

import com.example.Bookstore.dto.OrderDTO;
import com.example.Bookstore.dto.OrderItemDTO;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Integer orderStatus = null;
        if (status != null) {
            try {
                orderStatus = Integer.parseInt(status);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        Page<OrderDTO> orders = orderService.getAllOrders(customerId, orderStatus, 
                                                        paymentStatus, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String id) {
        Optional<OrderDTO> order = orderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        OrderDTO createdOrder = orderService.createOrder(orderDTO);
        return ResponseEntity.ok(createdOrder);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable String id, 
                                                @RequestBody OrderDTO orderDTO) {
        try {
            OrderDTO updatedOrder = orderService.updateOrder(id, orderDTO);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmOrder(@PathVariable String id) {
        try {
            orderService.confirmOrder(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        try {
            orderService.cancelOrder(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/pay")
    public ResponseEntity<Void> updatePaymentStatus(@PathVariable String id, 
                                                    @RequestBody PaymentRequest request) {
        try {
            orderService.updatePaymentStatus(id, request.getPaymentStatus());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDTO> addOrderItem(@PathVariable String id, 
                                                 @RequestBody OrderItemDTO orderItemDTO) {
        try {
            OrderDTO updatedOrder = orderService.addOrderItem(id, orderItemDTO);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeOrderItem(@PathVariable String id, 
                                                @PathVariable String itemId) {
        try {
            orderService.removeOrderItem(id, itemId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}/total")
    public ResponseEntity<Double> calculateOrderTotal(@PathVariable String id) {
        Double total = orderService.calculateOrderTotal(id);
        return ResponseEntity.ok(total);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByCustomer(@PathVariable String customerId) {
        List<OrderDTO> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/revenue")
    public ResponseEntity<Double> calculateRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        Double revenue = orderService.calculateRevenue(fromDate, toDate);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> countDeliveredOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        Long count = orderService.countDeliveredOrders(fromDate, toDate);
        return ResponseEntity.ok(count);
    }
    
    public static class PaymentRequest {
        private Integer paymentStatus;
        
        public Integer getPaymentStatus() {
            return paymentStatus;
        }
        
        public void setPaymentStatus(Integer paymentStatus) {
            this.paymentStatus = paymentStatus;
        }
    }
}
