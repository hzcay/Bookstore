package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.ShipmentDTO;
import com.example.Bookstore.entity.Employee;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.entity.Shipment;
import com.example.Bookstore.repository.EmployeeRepository;
import com.example.Bookstore.repository.OrderRepository;
import com.example.Bookstore.repository.ShipmentRepository;
import com.example.Bookstore.service.ShipmentService;
import com.example.Bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

	@Autowired
	private ShipmentRepository shipmentRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private BookService bookService;

	// LẤY DANH SÁCH / THỐNG KÊ
	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDTO> getShipmentsByShipper(String shipperId, Shipment.ShipmentStatus status, Pageable pageable) {
		Page<Shipment> shipments;

		if (status != null) {
		    shipments = shipmentRepository.findByShipper_EmployeeIdAndStatus(shipperId, status, pageable);
		} else {
		    shipments = shipmentRepository.findByShipper_EmployeeId(shipperId, pageable);
		}

		return shipments.map(this::convertToDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ShipmentDTO> getShipmentById(String shipmentId) {
		return shipmentRepository.findById(shipmentId).map(this::convertToDTO);
	}

	@Override
	public ShipmentDTO updateShipmentStatus(String shipmentId, String status) {
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy shipment"));
		shipment.setStatus(Shipment.ShipmentStatus.valueOf(status));
		shipmentRepository.save(shipment);
		return convertToDTO(shipment);
	}

	@Override
	@Transactional(readOnly = true)
	public long countByStatus(String shipperId, String status) {
		try {
			return shipmentRepository.countByShipper_EmployeeIdAndStatus(shipperId,
					Shipment.ShipmentStatus.valueOf(status));
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public double calculateCOD(String shipperId, LocalDate date) {
		return shipmentRepository.sumCODByDate(shipperId, date.atStartOfDay(), date.atTime(23, 59)).orElse(0.0);
	}

	// SHIPPER NHẬN ĐƠN HÀNG
	@Override
	public ShipmentDTO assignShipmentToShipper(String orderId, String shipperId) {
		// 1) Lấy order
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

		if (order.getStatus() != Order.OrderStatus.PROCESSING) {
			throw new RuntimeException("Đơn hàng không ở trạng thái PROCESSING, không thể nhận giao");
		}

		// 2) Lấy shipper
		Employee shipper = employeeRepository.findById(shipperId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy shipper"));

		// 3) TRỪ TỒN KHO khi shipper nhận đơn
		for (var orderItem : order.getOrderItems()) {
			bookService.updateStock(orderItem.getBook().getBookId(), -orderItem.getQuantity());
		}

		// 4) Cập nhật Order -> SHIPPING và flush để đồng bộ
		order.setStatus(Order.OrderStatus.SHIPPING);
		orderRepository.saveAndFlush(order);

		// 5) Tạo Shipment (KHÔNG gán shipmentId)
		Shipment shipment = new Shipment();
		// KHÔNG: shipment.setShipmentId(UUID.randomUUID().toString());
		shipment.setOrder(orderRepository.getReferenceById(order.getOrderId()));
		shipment.setShipper(shipper);
		shipment.setDeliveryAddress(order.getShippingAddress());
		shipment.setPickupTime(LocalDateTime.now());
		shipment.setStatus(Shipment.ShipmentStatus.PICKING);
		shipment.setCodAmount(order.getTotal() - order.getDiscount() + order.getShippingFee());

		// 6) Lưu (Hibernate sẽ INSERT và tự sinh UUID)
		shipmentRepository.save(shipment);

		return convertToDTO(shipment);
	}

	// ==========================
	// CẬP NHẬT TRẠNG THÁI SHIPMENT + ORDER
	// ==========================
	@Override
	public ShipmentDTO updateShipmentAndOrderStatus(String shipmentId, String shipmentStatus, String paymentMethod) {
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy shipment"));

		Order order = shipment.getOrder();
		Shipment.ShipmentStatus newStatus = Shipment.ShipmentStatus.valueOf(shipmentStatus);

		shipment.setStatus(newStatus);

		switch (newStatus) {
		case PICKING -> {
			// Khi shipper vừa nhận đơn, chỉ cập nhật trạng thái, KHÔNG gán thời gian
			shipment.setPickupTime(null);
			shipment.setDeliveryTime(null);
		}
		case OUT_FOR_DELIVERY -> {
			// Khi shipper bắt đầu đi giao → cập nhật pickup_time
			order.setStatus(Order.OrderStatus.SHIPPING);
			shipment.setPickupTime(LocalDateTime.now());
			shipment.setDeliveryTime(null);
		}
		case DELIVERED -> {
			// Khi giao thành công → cập nhật delivery_time + payment
			order.setStatus(Order.OrderStatus.DELIVERED);
			shipment.setDeliveryTime(LocalDateTime.now());
			if (paymentMethod != null && !paymentMethod.isBlank()) {
				order.setPaymentMethod(Order.PaymentMethod.valueOf(paymentMethod));
				order.setPaymentStatus(1); // Đã thanh toán
			}
		}
		case FAILED -> {
			// Giao thất bại → không có delivery_time
			order.setStatus(Order.OrderStatus.CANCELED);
			shipment.setDeliveryTime(null);
		}
		}

		shipmentRepository.save(shipment);
		orderRepository.save(order);

		return convertToDTO(shipment);
	}

	// CONVERT ENTITY -> DTO
	private ShipmentDTO convertToDTO(Shipment s) {
	    ShipmentDTO dto = new ShipmentDTO();
	    dto.setShipmentId(s.getShipmentId());
	    dto.setOrderId(s.getOrder() != null ? s.getOrder().getOrderId() : null);
	    dto.setShipperId(s.getShipper() != null ? s.getShipper().getEmployeeId() : null);
	    dto.setShipperName(s.getShipper() != null ? s.getShipper().getName() : null);

	    // Lấy tên khách hàng từ Order → Customer
	    if (s.getOrder() != null && s.getOrder().getCustomer() != null) {
	        dto.setCustomerName(s.getOrder().getCustomer().getName());
	    }

	    dto.setDeliveryAddress(s.getDeliveryAddress());
	    dto.setPickupTime(s.getPickupTime());
	    dto.setDeliveryTime(s.getDeliveryTime());
	    dto.setStatus(s.getStatus());
	    dto.setCodAmount(s.getCodAmount());

	    // Lấy paymentMethod từ Order
	    if (s.getOrder() != null && s.getOrder().getPaymentMethod() != null) {
	        dto.setPaymentMethod(s.getOrder().getPaymentMethod().name());
	    }

	    return dto;
	}

	
	@Override
	@Transactional(readOnly = true)
	public double calculateCODBetween(String shipperId, LocalDate from, LocalDate to) {
	    return shipmentRepository.sumCODByDate(shipperId, from.atStartOfDay(), to.atTime(23, 59)).orElse(0.0);
	}

	@Override
	@Transactional(readOnly = true)
	public long countDeliveredBetween(String shipperId, LocalDate from, LocalDate to) {
	    return shipmentRepository.countByShipper_EmployeeIdAndStatusAndDeliveryTimeBetween(
	            shipperId, Shipment.ShipmentStatus.DELIVERED, from.atStartOfDay(), to.atTime(23, 59));
	}

	@Override
	public Page<ShipmentDTO> getShipmentsByStatusAndDate(String shipperId, Shipment.ShipmentStatus status,
	                                                     LocalDate from, LocalDate to, Pageable pageable) {
	    Page<Shipment> shipments = shipmentRepository.findByShipper_EmployeeIdAndStatusAndDeliveryTimeBetween(
	            shipperId, status, from.atStartOfDay(), to.atTime(23, 59), pageable);
	    return shipments.map(this::convertToDTO);
	}
	
	@Override
	@Transactional(readOnly = true)
	public long countFailedBetween(String shipperId, LocalDate from, LocalDate to) {
	    return shipmentRepository.countByShipper_EmployeeIdAndStatusAndPickupTimeBetween(
	            shipperId, Shipment.ShipmentStatus.FAILED, from.atStartOfDay(), to.atTime(23, 59));
	}
}

