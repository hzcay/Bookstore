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
    
    @Query("SELECT o FROM Order o WHERE " +
           "(:customerId IS NULL OR o.customer.customerId = :customerId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:dateFrom IS NULL OR o.createAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR o.createAt <= :dateTo)")
    Page<Order> searchOrders(@Param("customerId") String customerId,
                            @Param("status") Integer status,
                            @Param("paymentStatus") Integer paymentStatus,
                            @Param("dateFrom") LocalDateTime dateFrom,
                            @Param("dateTo") LocalDateTime dateTo,
                            Pageable pageable);
    
    List<Order> findByCustomerCustomerIdAndStatus(String customerId, Integer status);
    
    Optional<Order> findByOrderIdAndStatus(String orderId, Integer status);
    
    @Query("SELECT SUM(o.total - o.discount + o.shippingFee) FROM Order o WHERE " +
           "o.status = 1 AND " +
           "o.createAt BETWEEN :fromDate AND :toDate")
    Double calculateRevenue(@Param("fromDate") LocalDateTime fromDate, 
                           @Param("toDate") LocalDateTime toDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 1 AND " +
           "o.createAt BETWEEN :fromDate AND :toDate")
    Long countDeliveredOrders(@Param("fromDate") LocalDateTime fromDate, 
                              @Param("toDate") LocalDateTime toDate);
}
