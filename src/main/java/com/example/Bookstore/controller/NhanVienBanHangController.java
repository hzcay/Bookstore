package com.example.Bookstore.controller;

import com.example.Bookstore.dto.BookDTO;
import com.example.Bookstore.dto.CustomerDTO;
import com.example.Bookstore.dto.ShipmentDTO;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.entity.Employee;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.CustomerRepository;
import com.example.Bookstore.repository.EmployeeRepository;
import com.example.Bookstore.service.OrderService;
import com.example.Bookstore.service.ShipmentService;
import com.example.Bookstore.service.InvoiceService;
import com.example.Bookstore.service.ReportService;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
@RequestMapping("/NhanVienBanHang")
public class NhanVienBanHangController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private InvoiceService invoiceService;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private SpringTemplateEngine templateEngine;

    // Helper method: Kiểm tra quyền cashier
    private void checkCashierAccess(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");
        String userRole = (String) session.getAttribute("userRole");
        
        if (userId == null || !"EMPLOYEE".equals(userType) || !"CASHIER".equals(userRole)) {
            throw new RuntimeException("Unauthorized access: Only CASHIER can access this page");
        }
    }

    @GetMapping("/home")
    public String home(HttpSession session) {
        checkCashierAccess(session);
        return "NhanVienBanHang/Home";
    }

    @GetMapping("/orders")
    public String viewOrders(@RequestParam(required = false) String search,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String fromDate,
                            @RequestParam(required = false) String toDate,
                            Model model,
                            HttpSession session) {
        checkCashierAccess(session);
        
        System.out.println("===== FILTER PARAMETERS =====");
        System.out.println("Search: " + search);
        System.out.println("Status: " + status);
        System.out.println("FromDate: " + fromDate);
        System.out.println("ToDate: " + toDate);
        
        List<com.example.Bookstore.dto.OrderDTO> orders;
        
        // Nếu có filter theo status
        if (status != null && !status.isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status);
                orders = orderService.getOrdersByStatus(orderStatus, org.springframework.data.domain.Pageable.unpaged()).getContent();
            } catch (IllegalArgumentException e) {
                orders = orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent();
            }
        } else {
            orders = orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        
        System.out.println("Orders after status filter: " + orders.size());
        
        // Nếu có search keyword
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            orders = orders.stream()
                .filter(order -> 
                    (order.getOrderId() != null && order.getOrderId().toLowerCase().contains(searchLower)) ||
                    (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(searchLower))
                )
                .collect(java.util.stream.Collectors.toList());
            System.out.println("Orders after search filter: " + orders.size());
        }
        
        // Lọc theo ngày
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            try {
                java.time.LocalDateTime fromDateTime = java.time.LocalDate.parse(fromDate).atStartOfDay();
                System.out.println("Filtering from: " + fromDateTime);
                
                int beforeSize = orders.size();
                orders = orders.stream()
                    .filter(order -> {
                        boolean match = order.getCreateAt() != null && 
                                !order.getCreateAt().isBefore(fromDateTime);
                        if (order.getCreateAt() != null) {
                            System.out.println("Order " + order.getOrderId() + " created at: " + order.getCreateAt() + " -> " + match);
                        }
                        return match;
                    })
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Orders after fromDate filter: " + beforeSize + " -> " + orders.size());
            } catch (Exception e) {
                System.err.println("Error parsing fromDate: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (toDate != null && !toDate.trim().isEmpty()) {
            try {
                java.time.LocalDateTime toDateTime = java.time.LocalDate.parse(toDate).atTime(23, 59, 59);
                System.out.println("Filtering to: " + toDateTime);
                
                int beforeSize = orders.size();
                orders = orders.stream()
                    .filter(order -> {
                        boolean match = order.getCreateAt() != null && 
                                !order.getCreateAt().isAfter(toDateTime);
                        if (order.getCreateAt() != null) {
                            System.out.println("Order " + order.getOrderId() + " created at: " + order.getCreateAt() + " -> " + match);
                        }
                        return match;
                    })
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Orders after toDate filter: " + beforeSize + " -> " + orders.size());
            } catch (Exception e) {
                System.err.println("Error parsing toDate: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Final orders count: " + orders.size());
        System.out.println("============================");
        
        model.addAttribute("orders", orders);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        
        return "NhanVienBanHang/Orders";
    }

    // Trang quản lý shipments
    @GetMapping("/shipments")
    public String viewShipments(@RequestParam(required = false) String shipmentStatus,
                               @RequestParam(required = false) String fromDate,
                               @RequestParam(required = false) String toDate,
                               Model model) {
        // Khởi tạo empty lists
        List<com.example.Bookstore.dto.OrderDTO> pendingOrders = new java.util.ArrayList<>();
        List<com.example.Bookstore.dto.OrderDTO> processingOrders = new java.util.ArrayList<>();
        List<ShipmentDTO> allShipments = new java.util.ArrayList<>();
        List<Employee> shippers = new java.util.ArrayList<>();
        
        try {
            System.out.println("=== DEBUG: viewShipments called ===");
            System.out.println("Shipment Status Filter: " + shipmentStatus);
            System.out.println("FromDate: " + fromDate);
            System.out.println("ToDate: " + toDate);
            
            // Kiểm tra services
            if (orderService == null) {
                throw new RuntimeException("OrderService is null!");
            }
            if (shipmentService == null) {
                throw new RuntimeException("ShipmentService is null!");
            }
            if (employeeRepository == null) {
                throw new RuntimeException("EmployeeRepository is null!");
            }
            
            // ✅ TỰ ĐỘNG ĐỒNG BỘ: Sửa order status theo shipment status
            shipmentService.syncOrderStatusWithShipments();
            System.out.println("✅ Đã đồng bộ order status với shipment status");
            
            // Lấy danh sách orders ở trạng thái PENDING
            System.out.println("Fetching PENDING orders...");
            pendingOrders = orderService.getOrdersByStatus(Order.OrderStatus.PENDING, org.springframework.data.domain.Pageable.unpaged()).getContent();
            System.out.println("PENDING orders count: " + (pendingOrders != null ? pendingOrders.size() : 0));
            
            // Lấy danh sách orders ở trạng thái PROCESSING
            System.out.println("Fetching PROCESSING orders...");
            processingOrders = orderService.getOrdersByStatus(Order.OrderStatus.PROCESSING, org.springframework.data.domain.Pageable.unpaged()).getContent();
            System.out.println("PROCESSING orders count: " + (processingOrders != null ? processingOrders.size() : 0));
            
            // Lấy tất cả shipments từ database
            System.out.println("Fetching all shipments...");
            allShipments = shipmentService.getAllShipments();
            System.out.println("All shipments count: " + (allShipments != null ? allShipments.size() : 0));
            
            // Lọc shipments theo trạng thái
            if (shipmentStatus != null && !shipmentStatus.isEmpty()) {
                try {
                    com.example.Bookstore.entity.Shipment.ShipmentStatus status = 
                        com.example.Bookstore.entity.Shipment.ShipmentStatus.valueOf(shipmentStatus);
                    allShipments = allShipments.stream()
                        .filter(s -> s.getStatus() == status)
                        .collect(java.util.stream.Collectors.toList());
                    System.out.println("Shipments after status filter: " + allShipments.size());
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid shipment status: " + shipmentStatus);
                }
            }
            
            // Lọc shipments theo ngày tạo
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                try {
                    java.time.LocalDateTime fromDateTime = java.time.LocalDate.parse(fromDate).atStartOfDay();
                    allShipments = allShipments.stream()
                        .filter(s -> s.getCreateAt() != null && !s.getCreateAt().isBefore(fromDateTime))
                        .collect(java.util.stream.Collectors.toList());
                    System.out.println("Shipments after fromDate filter: " + allShipments.size());
                } catch (Exception e) {
                    System.err.println("Error parsing fromDate: " + e.getMessage());
                }
            }
            
            if (toDate != null && !toDate.trim().isEmpty()) {
                try {
                    java.time.LocalDateTime toDateTime = java.time.LocalDate.parse(toDate).atTime(23, 59, 59);
                    allShipments = allShipments.stream()
                        .filter(s -> s.getCreateAt() != null && !s.getCreateAt().isAfter(toDateTime))
                        .collect(java.util.stream.Collectors.toList());
                    System.out.println("Shipments after toDate filter: " + allShipments.size());
                } catch (Exception e) {
                    System.err.println("Error parsing toDate: " + e.getMessage());
                }
            }
            
            // Lấy danh sách shipper
            System.out.println("Fetching active shippers...");
            shippers = employeeRepository.findActiveShippers();
            System.out.println("Active shippers count: " + (shippers != null ? shippers.size() : 0));
            
            System.out.println("=== DEBUG: Data fetched successfully ===");
            
        } catch (Exception e) {
            System.err.println("=== ERROR in viewShipments ===");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        // Đảm bảo không null
        if (pendingOrders == null) pendingOrders = new java.util.ArrayList<>();
        if (processingOrders == null) processingOrders = new java.util.ArrayList<>();
        if (allShipments == null) allShipments = new java.util.ArrayList<>();
        if (shippers == null) shippers = new java.util.ArrayList<>();
        
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("shipments", allShipments);
        model.addAttribute("shippers", shippers);
        model.addAttribute("selectedShipmentStatus", shipmentStatus);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        
        System.out.println("=== DEBUG: Returning view ===");
        return "NhanVienBanHang/Shipments";
    }
    
    @GetMapping("/shipments-test")
    public String viewShipmentsTest(@RequestParam(required = false) String shipmentStatus,
                                   @RequestParam(required = false) String fromDate,
                                   @RequestParam(required = false) String toDate,
                                   Model model) {
        // Gọi method chính
        viewShipments(shipmentStatus, fromDate, toDate, model);
        // Đổi view sang test version
        return "NhanVienBanHang/ShipmentsTest";
    }

    // API: Lấy chi tiết đơn hàng
    @GetMapping("/orders/{orderId}/detail")
    @ResponseBody
    public ResponseEntity<?> getOrderDetail(@PathVariable String orderId) {
        try {
            java.util.Optional<com.example.Bookstore.dto.OrderDTO> orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isPresent()) {
                return ResponseEntity.ok(orderOpt.get());
            } else {
                return ResponseEntity.badRequest().body("{\"error\": \"Không tìm thấy đơn hàng\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    // API: Xác nhận đơn hàng (PENDING -> PROCESSING)
    @PostMapping("/orders/{orderId}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmOrder(@PathVariable String orderId) {
        try {
            orderService.confirmOrder(orderId);
            return ResponseEntity.ok().body("{\"message\": \"Đơn hàng đã được xác nhận\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // API: Gán shipper cho đơn hàng
    @PostMapping("/orders/{orderId}/assign-shipper")
    @ResponseBody
    public ResponseEntity<?> assignShipper(@PathVariable String orderId, @RequestParam String shipperId) {
        try {
            ShipmentDTO shipment = shipmentService.assignShipmentToShipper(orderId, shipperId);
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // API: Xác nhận shipper đã lấy hàng và bắt đầu giao (PICKING -> OUT_FOR_DELIVERY/SHIPPING)
    @PostMapping("/shipments/{shipmentId}/confirm-shipping")
    @ResponseBody
    public ResponseEntity<?> confirmShipping(@PathVariable String shipmentId) {
        try {
            ShipmentDTO shipment = shipmentService.updateShipmentStatus(shipmentId, "OUT_FOR_DELIVERY");
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // API: Xác nhận giao hàng thành công (OUT_FOR_DELIVERY -> DELIVERED)
    @PostMapping("/shipments/{shipmentId}/confirm-delivery")
    @ResponseBody
    public ResponseEntity<?> confirmDelivery(@PathVariable String shipmentId) {
        try {
            ShipmentDTO shipment = shipmentService.updateShipmentStatus(shipmentId, "DELIVERED");
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    // API test: Kiểm tra shipments
    @GetMapping("/api/test-shipments")
    @ResponseBody
    public ResponseEntity<?> testShipments() {
        try {
            List<ShipmentDTO> allShipments = shipmentService.getAllShipments();
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("count", allShipments != null ? allShipments.size() : 0);
            result.put("shipments", allShipments);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", e.toString());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // API test: Kiểm tra danh sách shippers
    @GetMapping("/api/test-shippers")
    @ResponseBody
    public ResponseEntity<?> testShippers() {
        try {
            List<Employee> allShippers = employeeRepository.findActiveShippers();
            List<Employee> allEmployees = employeeRepository.findAll();
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("activeShippers", allShippers);
            result.put("activeShippersCount", allShippers != null ? allShippers.size() : 0);
            result.put("allEmployees", allEmployees);
            result.put("allEmployeesCount", allEmployees != null ? allEmployees.size() : 0);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", e.toString());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // Trang danh sách sách
    @GetMapping("/books")
    public String viewBooks(@RequestParam(required = false) String search, Model model) {
        try {
            System.out.println("===== BOOKS PAGE =====");
            System.out.println("Search: " + search);
            
            // Lấy tất cả sách
            List<Book> books = bookRepository.findAll();
            
            // Chuyển sang DTO và tính tồn kho
            List<BookDTO> bookDTOs = books.stream()
                .map(this::convertBookToDTO)
                .collect(java.util.stream.Collectors.toList());
            
            // Lọc theo search keyword
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                bookDTOs = bookDTOs.stream()
                    .filter(book -> 
                        (book.getTitle() != null && book.getTitle().toLowerCase().contains(searchLower)) ||
                        (book.getAuthorName() != null && book.getAuthorName().toLowerCase().contains(searchLower)) ||
                        (book.getBookId() != null && book.getBookId().toLowerCase().contains(searchLower))
                    )
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Books after search filter: " + bookDTOs.size());
            }
            
            model.addAttribute("books", bookDTOs);
            model.addAttribute("searchKeyword", search);
            
            System.out.println("Total books: " + bookDTOs.size());
            
        } catch (Exception e) {
            System.err.println("Error in viewBooks: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "NhanVienBanHang/Books";
    }
    
    private BookDTO convertBookToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setBookId(book.getBookId());
        dto.setTitle(book.getTitle());
        
        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getCategoryId());
            dto.setCategoryName(book.getCategory().getName());
        }
        
        if (book.getAuthor() != null) {
            dto.setAuthorId(book.getAuthor().getAuthorId());
            dto.setAuthorName(book.getAuthor().getName());
        }
        
        if (book.getPublisher() != null) {
            dto.setPublisherId(book.getPublisher().getPublisherId());
            dto.setPublisherName(book.getPublisher().getName());
        }
        
        dto.setImportPrice(book.getImportPrice());
        dto.setSalePrice(book.getSalePrice());
        dto.setQuantity(book.getQuantity());
        dto.setCreateAt(book.getCreateAt());
        dto.setUpdateAt(book.getUpdateAt());
        dto.setStatus(book.getStatus());
        
        // Sử dụng Book.quantity làm tồn kho thực tế
        dto.setStockQuantity(book.getQuantity());
        
        return dto;
    }
    
    // API: Lấy chi tiết sách
    @GetMapping("/books/{bookId}/detail")
    @ResponseBody
    public ResponseEntity<?> getBookDetail(@PathVariable String bookId) {
        try {
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + bookId));
            
            BookDTO dto = convertBookToDTO(book);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    // API: Gợi ý tìm kiếm sách (autocomplete)
    @GetMapping("/books/search/suggestions")
    @ResponseBody
    public ResponseEntity<?> searchSuggestions(@RequestParam String q) {
        try {
            if (q == null || q.trim().length() < 2) {
                return ResponseEntity.ok(java.util.List.of());
            }
            
            String searchLower = q.toLowerCase().trim();
            List<Book> books = bookRepository.findAll();
            
            List<java.util.Map<String, Object>> suggestions = books.stream()
                .filter(book -> 
                    (book.getTitle() != null && book.getTitle().toLowerCase().contains(searchLower)) ||
                    (book.getAuthor() != null && book.getAuthor().getName() != null && 
                     book.getAuthor().getName().toLowerCase().contains(searchLower)) ||
                    (book.getBookId() != null && book.getBookId().toLowerCase().contains(searchLower))
                )
                .limit(10)
                .map(book -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("bookId", book.getBookId());
                    map.put("title", book.getTitle());
                    map.put("authorName", book.getAuthor() != null ? book.getAuthor().getName() : "");
                    map.put("salePrice", book.getSalePrice());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== CUSTOMER MANAGEMENT ====================
    
    // Trang danh sách khách hàng
    @GetMapping("/customers")
    public String viewCustomers(@RequestParam(required = false) String search, Model model) {
        try {
            System.out.println("===== CUSTOMERS PAGE =====");
            System.out.println("Search: " + search);
            
            // Lấy tất cả khách hàng
            List<com.example.Bookstore.entity.Customer> customers = 
                customerRepository.findAll();
            
            // Chuyển sang DTO
            List<CustomerDTO> customerDTOs = customers.stream()
                .map(this::convertCustomerToDTO)
                .collect(java.util.stream.Collectors.toList());
            
            // Lọc theo search keyword
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                customerDTOs = customerDTOs.stream()
                    .filter(customer -> 
                        (customer.getName() != null && customer.getName().toLowerCase().contains(searchLower)) ||
                        (customer.getPhone() != null && customer.getPhone().toLowerCase().contains(searchLower)) ||
                        (customer.getCustomerId() != null && customer.getCustomerId().toLowerCase().contains(searchLower)) ||
                        (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(searchLower))
                    )
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Customers after search filter: " + customerDTOs.size());
            }
            
            model.addAttribute("customers", customerDTOs);
            model.addAttribute("searchKeyword", search);
            
            System.out.println("Total customers: " + customerDTOs.size());
            
        } catch (Exception e) {
            System.err.println("Error in viewCustomers: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "NhanVienBanHang/Customers";
    }
    
    // API: Lấy chi tiết khách hàng
    @GetMapping("/customers/{customerId}/detail")
    @ResponseBody
    public ResponseEntity<?> getCustomerDetail(@PathVariable String customerId) {
        try {
            com.example.Bookstore.entity.Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));
            
            CustomerDTO dto = convertCustomerToDTO(customer);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    // API: Gợi ý tìm kiếm khách hàng (autocomplete)
    @GetMapping("/customers/search/suggestions")
    @ResponseBody
    public ResponseEntity<?> searchCustomerSuggestions(@RequestParam String q) {
        try {
            if (q == null || q.trim().length() < 2) {
                return ResponseEntity.ok(java.util.List.of());
            }
            
            String searchLower = q.toLowerCase().trim();
            List<com.example.Bookstore.entity.Customer> customers = customerRepository.findAll();
            
            List<java.util.Map<String, Object>> suggestions = customers.stream()
                .filter(customer -> 
                    (customer.getName() != null && customer.getName().toLowerCase().contains(searchLower)) ||
                    (customer.getPhone() != null && customer.getPhone().toLowerCase().contains(searchLower)) ||
                    (customer.getCustomerId() != null && customer.getCustomerId().toLowerCase().contains(searchLower))
                )
                .limit(10)
                .map(customer -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("customerId", customer.getCustomerId());
                    map.put("name", customer.getName());
                    map.put("phone", customer.getPhone());
                    map.put("points", customer.getPoints());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/orders/{orderId}/invoice-pdf")
    @Transactional
    public ResponseEntity<byte[]> downloadInvoicePDF(@PathVariable String orderId) {
        try {
            Order order = orderService.getOrderById(orderId)
                .map(dto -> {
                    Order entity = new Order();
                    entity.setOrderId(dto.getOrderId());
                    entity.setTotal(dto.getTotal());
                    entity.setDiscount(dto.getDiscount());
                    entity.setShippingFee(dto.getShippingFee());
                    entity.setShippingAddress(dto.getShippingAddress());
                    entity.setCreateAt(dto.getCreateAt());
                    entity.setStatus(dto.getStatus());
                    
                    if (dto.getCustomerId() != null) {
                        com.example.Bookstore.entity.Customer customer = new com.example.Bookstore.entity.Customer();
                        customer.setName(dto.getCustomerName());
                        customer.setPhone(dto.getCustomerPhone());
                        entity.setCustomer(customer);
                    }
                    
                    if (dto.getOrderItems() != null) {
                        java.util.List<com.example.Bookstore.entity.OrderItem> items = dto.getOrderItems().stream()
                            .map(itemDto -> {
                                com.example.Bookstore.entity.OrderItem item = new com.example.Bookstore.entity.OrderItem();
                                item.setQuantity(itemDto.getQuantity());
                                item.setPrice(itemDto.getPrice());
                                
                                com.example.Bookstore.entity.Book book = new com.example.Bookstore.entity.Book();
                                book.setTitle(itemDto.getBookTitle());
                                item.setBook(book);
                                
                                return item;
                            })
                            .collect(java.util.stream.Collectors.toList());
                        entity.setOrderItems(items);
                    }
                    
                    return entity;
                })
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));
            
            // Generate PDF
            byte[] pdfBytes = invoiceService.generateInvoicePDF(order);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "hoadon-" + orderId + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Loi: " + e.getMessage()).getBytes());
        }
    }
    
    private CustomerDTO convertCustomerToDTO(com.example.Bookstore.entity.Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(customer.getCustomerId());
        dto.setName(customer.getName());
        dto.setPhone(customer.getPhone());
        dto.setEmail(customer.getEmail());
        dto.setAddress(customer.getAddress());
        dto.setPoints(customer.getPoints());
        dto.setStatus(customer.getStatus());
        return dto;
    }
    
    /**
     * Báo cáo doanh thu - chỉ tính đơn DELIVERED
     */
    @GetMapping("/reports")
    public String viewReports(@RequestParam(required = false) String fromDate,
                             @RequestParam(required = false) String toDate,
                             Model model,
                             HttpSession session) {
        // Kiểm tra quyền cashier
        checkCashierAccess(session);
        
        try {
            // Mặc định: báo cáo hôm nay
            java.time.LocalDateTime startDate;
            java.time.LocalDateTime endDate;
            
            if (fromDate != null && !fromDate.trim().isEmpty() && 
                toDate != null && !toDate.trim().isEmpty()) {
                // Parse từ input
                startDate = java.time.LocalDate.parse(fromDate).atStartOfDay();
                endDate = java.time.LocalDate.parse(toDate).atTime(23, 59, 59);
            } else {
                // Mặc định: hôm nay
                startDate = java.time.LocalDate.now().atStartOfDay();
                endDate = java.time.LocalDateTime.now();
                fromDate = java.time.LocalDate.now().toString();
                toDate = java.time.LocalDate.now().toString();
            }
            
            // Lấy số liệu từ ReportService
            Long totalOrders = reportService.countTotalOrders(startDate, endDate);
            Double totalRevenue = reportService.calculateTotalRevenue(startDate, endDate);
            
            // Đưa vào model
            model.addAttribute("totalOrders", totalOrders != null ? totalOrders : 0);
            model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalRevenue", 0.0);
            model.addAttribute("fromDate", java.time.LocalDate.now().toString());
            model.addAttribute("toDate", java.time.LocalDate.now().toString());
        }
        
        return "NhanVienBanHang/Reports";
    }
    
    /**
     * Xuất báo cáo PDF
     */
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> exportReportPDF(@RequestParam String fromDate,
                                               @RequestParam String toDate,
                                               HttpSession session) {
        // Kiểm tra quyền cashier
        checkCashierAccess(session);
        
        try {
            // Parse dates
            java.time.LocalDateTime startDate = java.time.LocalDate.parse(fromDate).atStartOfDay();
            java.time.LocalDateTime endDate = java.time.LocalDate.parse(toDate).atTime(23, 59, 59);
            
            // Lấy dữ liệu
            Long totalOrders = reportService.countTotalOrders(startDate, endDate);
            Double totalRevenue = reportService.calculateTotalRevenue(startDate, endDate);
            
            // Tạo context cho Thymeleaf
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("fromDate", fromDate);
            context.setVariable("toDate", toDate);
            context.setVariable("totalOrders", totalOrders != null ? totalOrders : 0);
            context.setVariable("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            context.setVariable("exportTime", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            ));
            
            // Render template
            String html = templateEngine.process("NhanVienBanHang/report-template", context);
            
            // Convert to PDF
            byte[] pdfBytes = invoiceService.generatePDFFromHTML(html);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "bao-cao-doanh-thu.pdf");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Lỗi: " + e.getMessage()).getBytes());
        }
    }
}
