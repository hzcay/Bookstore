package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Shipment;
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
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    
    // Legacy methods (giữ lại để tương thích)
    @Query("SELECT s FROM Shipment s WHERE " +
           "(:shipperId IS NULL OR s.shipper.employeeId = :shipperId) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<Shipment> searchShipments(@Param("shipperId") String shipperId,
                                  @Param("status") Shipment.ShipmentStatus status);
    
    List<Shipment> findByShipperEmployeeId(String shipperId);
    
    Optional<Shipment> findByOrderOrderId(String orderId);
    
    Optional<Shipment> findByShipmentIdAndStatus(String shipmentId, Shipment.ShipmentStatus status);
    
    List<Shipment> findByStatus(Shipment.ShipmentStatus status);
    
    // New methods from Bookstore (2) for shipper functionality
    // Lấy danh sách shipment theo shipper
    Page<Shipment> findByShipper_EmployeeId(String shipperId, Pageable pageable);

    // Lấy danh sách shipment theo shipper + trạng thái
    Page<Shipment> findByShipper_EmployeeIdAndStatus(String shipperId, Shipment.ShipmentStatus status, Pageable pageable);

    // Đếm số lượng shipment theo trạng thái
    long countByShipper_EmployeeIdAndStatus(String shipperId, Shipment.ShipmentStatus status);

    long countByShipper_EmployeeIdAndStatusAndDeliveryTimeBetween(
            String shipperId, Shipment.ShipmentStatus status, LocalDateTime from, LocalDateTime to);

    long countByShipper_EmployeeIdAndStatusAndPickupTimeBetween(
            String shipperId, Shipment.ShipmentStatus status, LocalDateTime from, LocalDateTime to);

    Page<Shipment> findByShipper_EmployeeIdAndStatusAndDeliveryTimeBetween(
            String shipperId, Shipment.ShipmentStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    // Tính tổng COD trong ngày 
    @Query("SELECT SUM(s.codAmount) FROM Shipment s WHERE s.shipper.employeeId = :shipperId " +
           "AND s.deliveryTime BETWEEN :from AND :to AND s.status = 'DELIVERED'")
    Optional<Double> sumCODByDate(@Param("shipperId") String shipperId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
    
    // Lấy tất cả shipments với details (JOIN FETCH)
    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH s.shipper")
    List<Shipment> findAllWithDetails();
    
    // Lấy shipments theo status với details
    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH s.shipper WHERE s.status = :status")
    List<Shipment> findByStatusWithDetails(@Param("status") Shipment.ShipmentStatus status);
    
    // Lấy shipments theo shipper với details
    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH s.shipper WHERE s.shipper.employeeId = :shipperId")
    List<Shipment> findByShipperEmployeeIdWithDetails(@Param("shipperId") String shipperId);

        List<Shipment> findByOrderOrderIdOrderByPickupTimeAsc(String orderId);
}
