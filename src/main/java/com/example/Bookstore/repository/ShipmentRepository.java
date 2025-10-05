package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    
    @Query("SELECT s FROM Shipment s WHERE " +
           "(:shipperId IS NULL OR s.shipper.employeeId = :shipperId) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<Shipment> searchShipments(@Param("shipperId") String shipperId,
                                  @Param("status") Shipment.ShipmentStatus status);
    
    List<Shipment> findByShipperEmployeeId(String shipperId);
    
    Optional<Shipment> findByOrderOrderId(String orderId);
    
    Optional<Shipment> findByShipmentIdAndStatus(String shipmentId, Shipment.ShipmentStatus status);
    
    List<Shipment> findByStatus(Shipment.ShipmentStatus status);
}
