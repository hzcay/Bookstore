package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    @Query(value = "SELECT * FROM orders o ORDER BY o.create_at DESC", nativeQuery = true)
    Page<Order> searchOrders(Pageable pageable);
    
    List<Order> findByCustomerCustomerIdAndStatus(String customerId, Order.OrderStatus status);
    
    Optional<Order> findByOrderIdAndStatus(String orderId, Order.OrderStatus status);
    
    // For shipper: get orders by status
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    @Query("SELECT SUM(o.total - o.discount + o.shippingFee) FROM Order o WHERE " +
           "o.status = com.example.Bookstore.entity.Order$OrderStatus.DELIVERED AND " +
           "o.createAt BETWEEN :fromDate AND :toDate")
    Double calculateRevenue(@Param("fromDate") LocalDateTime fromDate, 
                           @Param("toDate") LocalDateTime toDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = com.example.Bookstore.entity.Order$OrderStatus.DELIVERED AND " +
           "o.createAt BETWEEN :fromDate AND :toDate")
    Long countDeliveredOrders(@Param("fromDate") LocalDateTime fromDate, 
                              @Param("toDate") LocalDateTime toDate);
}
