package com.example.Bookstore.controller;

import com.example.Bookstore.dto.ShipmentDTO;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.entity.Shipment;
import com.example.Bookstore.service.OrderService;
import com.example.Bookstore.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

	@Autowired
	private ShipmentService shipmentService;

	@Autowired
	private OrderService orderService;

	// GIẢ LẬP SHIPPER ĐANG ĐĂNG NHẬP
	private String getCurrentShipperId(HttpSession session) {
		String userId = (String) session.getAttribute("userId");
		String userType = (String) session.getAttribute("userType");
		String userRole = (String) session.getAttribute("userRole");
		
		if (userId == null || !"EMPLOYEE".equals(userType) || !"SHIPPER".equals(userRole)) {
			throw new RuntimeException("Unauthorized access");
		}
		
		return userId;
	}

	// TRANG CHÍNH: DANH SÁCH ĐƠN HÀNG
	@GetMapping("/shipments")
	public String shipmentsPage(
	        Model model,
	        HttpSession session,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(required = false) String status,
	        @RequestParam(required = false) String from,
	        @RequestParam(required = false) String to
	) {
	    String shipperId = getCurrentShipperId(session);
	    String shipperName = (String) session.getAttribute("userName");
	    model.addAttribute("shipperName", shipperName);
	    Pageable pageable = PageRequest.of(page, size);

	    try {
	        // Xử lý lọc theo trạng thái
	        Shipment.ShipmentStatus filterStatus = null;
	        if (status != null && !status.equalsIgnoreCase("ALL") && !status.isBlank()) {
	            filterStatus = Shipment.ShipmentStatus.valueOf(status);
	        }

	        // Xử lý khoảng thời gian (chỉ khi trạng thái = DELIVERED)
	        LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : null;
	        LocalDate toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : null;

	        Page<ShipmentDTO> shipments;
	        if (filterStatus == Shipment.ShipmentStatus.DELIVERED && fromDate != null && toDate != null) {
	            shipments = shipmentService.getShipmentsByStatusAndDate(shipperId, filterStatus, fromDate, toDate, pageable);
	        } else if (filterStatus != null) {
	            shipments = shipmentService.getShipmentsByShipper(shipperId, filterStatus, pageable);
	        } else {
	            shipments = shipmentService.getShipmentsByShipper(shipperId, null, pageable);
	        }

	        // Đơn hàng chờ nhận (PROCESSING)
	        model.addAttribute("pendingOrders", orderService
	                .getOrdersByStatus(Order.OrderStatus.PROCESSING, pageable).getContent());

	        // Gửi dữ liệu ra view
	        model.addAttribute("shipments", shipments);
	        model.addAttribute("selectedStatus", status != null ? status : "ALL");
	        model.addAttribute("fromDate", fromDate);
	        model.addAttribute("toDate", toDate);

	    } catch (Exception e) {
	        model.addAttribute("error", "Lỗi tải dữ liệu: " + e.getMessage());
	    }

	    model.addAttribute("activePage", "shipments");
	    return "shipper/shipments";
	}


	// NHẬN GIAO ĐƠN HÀNG (SHIPPER BẤM "NHẬN")
	@PostMapping("/accept-order/{orderId}")
	public String acceptOrder(@PathVariable String orderId, RedirectAttributes redirect, HttpSession session) {
		String shipperId = getCurrentShipperId(session);
		try {
			shipmentService.assignShipmentToShipper(orderId, shipperId);
			redirect.addFlashAttribute("success", "✅ Nhận đơn hàng thành công!");
		} catch (Exception e) {
			redirect.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
		}
		return "redirect:/shipper/shipments";
	}

	// XEM CHI TIẾT MỘT SHIPMENT
	@GetMapping("/shipments/{shipmentId}")
	public String shipmentDetail(@PathVariable String shipmentId, Model model, RedirectAttributes redirect, HttpSession session) {
		String shipperId = getCurrentShipperId(session);
		try {
			ShipmentDTO shipment = shipmentService.getShipmentById(shipmentId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy shipment"));

			// 🔒 Chỉ cho phép shipper xem shipment của chính họ
			if (!shipperId.equals(shipment.getShipperId())) {
				redirect.addFlashAttribute("error", "Bạn không có quyền xem shipment này!");
				return "redirect:/shipper/shipments";
			}

			model.addAttribute("shipment", shipment);
		} catch (Exception e) {
			redirect.addFlashAttribute("error", "Lỗi: " + e.getMessage());
			return "redirect:/shipper/shipments";
		}

		model.addAttribute("activePage", "shipments");
		return "shipper/shipment-detail";
	}

	// CẬP NHẬT TRẠNG THÁI GIAO HÀNG
	@PostMapping("/shipments/update/{shipmentId}")
	public String updateShipment(@PathVariable String shipmentId, @RequestParam String status,
			@RequestParam(required = false) String paymentMethod, RedirectAttributes redirect, HttpSession session) {
		String shipperId = getCurrentShipperId(session);
		try {
			ShipmentDTO shipment = shipmentService.getShipmentById(shipmentId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy shipment"));

			// 🔒 Kiểm tra quyền cập nhật shipment
			if (!shipperId.equals(shipment.getShipperId())) {
				redirect.addFlashAttribute("error", "Bạn không thể cập nhật shipment của người khác!");
				return "redirect:/shipper/shipments";
			}

			shipmentService.updateShipmentAndOrderStatus(shipmentId, status, paymentMethod);
			redirect.addFlashAttribute("success", "✅ Cập nhật trạng thái thành công!");
		} catch (Exception e) {
			redirect.addFlashAttribute("error", "❌ Lỗi cập nhật: " + e.getMessage());
		}
		return "redirect:/shipper/shipments";
	}

	// DASHBOARD TRANG CHỦ
	@GetMapping({ "", "/", "/home" })
	public String home(Model model, HttpSession session) {
		String shipperId = getCurrentShipperId(session);
		String shipperName = (String) session.getAttribute("userName");
		model.addAttribute("shipperName", shipperName);
		
		try {
			// Đếm đơn đang nhận (PICKING)
			long picking = shipmentService.countByStatus(shipperId, "PICKING");

			long delivering = shipmentService.countByStatus(shipperId, "OUT_FOR_DELIVERY");
			long delivered = shipmentService.countByStatus(shipperId, "DELIVERED");
			long failed = shipmentService.countByStatus(shipperId, "FAILED");
			double todayCOD = shipmentService.calculateCOD(shipperId, LocalDate.now());

			// Truyền ra view
			model.addAttribute("picking", picking);
			model.addAttribute("delivering", delivering);
			model.addAttribute("delivered", delivered);
			model.addAttribute("failed", failed);
			model.addAttribute("todayCOD", todayCOD);
		} catch (Exception e) {
			model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
		}

		model.addAttribute("activePage", "home");
		return "shipper/home";
	}

	// TRANG BÁO CÁO CỦA SHIPPER
	@GetMapping("/report")
	public String report(
	        @RequestParam(required = false) String from,
	        @RequestParam(required = false) String to,
	        Model model,
	        HttpSession session
	) {
	    String shipperId = getCurrentShipperId(session);
	    String shipperName = (String) session.getAttribute("userName");
	    model.addAttribute("shipperName", shipperName);
	    
	    try {
	        // ✅ Nếu chưa chọn ngày -> mặc định 7 ngày gần nhất
	        LocalDate toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : LocalDate.now();
	        LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : toDate.minusDays(7);

	        // Gọi service để lấy dữ liệu
	        double totalCOD = shipmentService.calculateCODBetween(shipperId, fromDate, toDate);
	        long deliveredCount = shipmentService.countDeliveredBetween(shipperId, fromDate, toDate);
	        long failedCount = shipmentService.countFailedBetween(shipperId, fromDate, toDate);

	        // Gửi ra view
	        model.addAttribute("fromDate", fromDate);
	        model.addAttribute("toDate", toDate);
	        model.addAttribute("totalCOD", totalCOD);
	        model.addAttribute("deliveredCount", deliveredCount);
	        model.addAttribute("failedCount", failedCount);

	    } catch (Exception e) {
	        model.addAttribute("error", "Không thể tải báo cáo: " + e.getMessage());
	    }

	    model.addAttribute("activePage", "report");
	    return "shipper/report";
	}

}

