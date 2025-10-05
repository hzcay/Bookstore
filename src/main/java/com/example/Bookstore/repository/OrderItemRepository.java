package com.example.Bookstore.repository;

import com.example.Bookstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    
    List<OrderItem> findByOrderOrderId(String orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") String orderId);
    
    @Query("SELECT SUM(oi.quantity * oi.price) FROM OrderItem oi WHERE oi.order.orderId = :orderId")
    Double calculateOrderTotal(@Param("orderId") String orderId);
}
