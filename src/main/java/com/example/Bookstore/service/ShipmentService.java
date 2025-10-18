package com.example.Bookstore.service;

import com.example.Bookstore.dto.ShipmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.Bookstore.entity.Shipment;

import java.time.LocalDate;
import java.util.Optional;

public interface ShipmentService {

    /**
     * Lấy danh sách shipment theo shipper
     */
    Page<ShipmentDTO> getShipmentsByShipper(String shipperId, Shipment.ShipmentStatus status, Pageable pageable);

    /**
     * Lấy chi tiết shipment theo ID
     */
    Optional<ShipmentDTO> getShipmentById(String shipmentId);

    /**
     * Cập nhật trạng thái shipment (chỉ riêng shipment, không động đến order)
     */
    ShipmentDTO updateShipmentStatus(String shipmentId, String status);

    /**
     * Đếm số lượng shipment theo trạng thái
     */
    long countByStatus(String shipperId, String status);

    /**
     * Tính tổng COD của shipper trong ngày
     */
    double calculateCOD(String shipperId, LocalDate date);

    /**
     * Khi shipper nhận đơn hàng đang ở trạng thái PROCESSING
     * -> tạo shipment mới, set PICKING, đồng bộ order sang SHIPPING
     */
    ShipmentDTO assignShipmentToShipper(String orderId, String shipperId);

    /**
     * Cập nhật shipment và đồng bộ trạng thái order tương ứng
     * (DELIVERED, FAILED, OUT_FOR_DELIVERY, v.v.)
     */
    ShipmentDTO updateShipmentAndOrderStatus(String shipmentId, String shipmentStatus, String paymentMethod);
    
    Page<ShipmentDTO> getShipmentsByStatusAndDate(String shipperId, Shipment.ShipmentStatus status,
            LocalDate from, LocalDate to, Pageable pageable);
    
    double calculateCODBetween(String shipperId, LocalDate from, LocalDate to);
    long countDeliveredBetween(String shipperId, LocalDate from, LocalDate to);
    long countFailedBetween(String shipperId, LocalDate from, LocalDate to);
}

