package com.example.Bookstore.service.impl;

import com.example.Bookstore.controller.AuthController;
import com.example.Bookstore.dto.*;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.entity.OrderItem;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.CustomerRepository;
import com.example.Bookstore.repository.OrderRepository;
import com.example.Bookstore.repository.PromotionRepository;
import com.example.Bookstore.repository.ShipmentRepository;
import com.example.Bookstore.service.CartService;
import com.example.Bookstore.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private CartService cartService;
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.searchOrders(pageable)
                .map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDTO> getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDTO);
    }
    
    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        Order order = convertToEntity(orderDTO);
        order = orderRepository.save(order);
        return convertToDTO(order);
    }
    
    @Override
    public OrderDTO updateOrder(String orderId, OrderDTO orderDTO) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        updateEntityFromDTO(existingOrder, orderDTO);
        existingOrder = orderRepository.save(existingOrder);
        return convertToDTO(existingOrder);
    }
    
    @Override
    public void confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.PROCESSING);
            orderRepository.save(order);
        } else {
            throw new RuntimeException("Order cannot be confirmed in current status");
        }
    }
    
    @Override
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CANCELED);
            orderRepository.save(order);
        } else {
            throw new RuntimeException("Order cannot be canceled in current status");
        }
    }
    
    @Override
    public void updatePaymentStatus(String orderId, Integer paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }
    
    @Override
    public OrderDTO addOrderItem(String orderId, OrderItemDTO orderItemDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot modify order items after confirmation");
        }
        
        return convertToDTO(order);
    }
    
    @Override
    public void removeOrderItem(String orderId, String itemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot modify order items after confirmation");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double calculateOrderTotal(String orderId) {
        return orderRepository.findById(orderId)
                .map(order -> order.getTotal() - order.getDiscount() + order.getShippingFee())
                .orElse(0.0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerCustomerIdAndStatus(customerId, Order.OrderStatus.DELIVERED)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double calculateRevenue(LocalDateTime fromDate, LocalDateTime toDate) {
        return orderRepository.calculateRevenue(fromDate, toDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long countDeliveredOrders(LocalDateTime fromDate, LocalDateTime toDate) {
        return orderRepository.countDeliveredOrders(fromDate, toDate);
    }
    
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setTotal(order.getTotal());
        dto.setDiscount(order.getDiscount());
        dto.setShippingFee(order.getShippingFee());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setCreateAt(order.getCreateAt());
        dto.setUpdateAt(order.getUpdateAt());
        dto.setStatus(order.getStatus());
        dto.setShippingAddress(order.getShippingAddress());
        
        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getCustomerId());
            dto.setCustomerName(order.getCustomer().getName());
        }
        
        if (order.getOrderItems() != null) {
            dto.setOrderItems(order.getOrderItems().stream()
                    .map(this::convertOrderItemToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private OrderItemDTO convertOrderItemToDTO(com.example.Bookstore.entity.OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(orderItem.getOrderItemId());
        dto.setOrderId(orderItem.getOrder().getOrderId());
        dto.setBookId(orderItem.getBook().getBookId());
        dto.setBookTitle(orderItem.getBook().getTitle());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setSubtotal(orderItem.getQuantity() * orderItem.getPrice());
        return dto;
    }
    
    private Order convertToEntity(OrderDTO dto) {
        Order order = new Order();
        order.setOrderId(dto.getOrderId());
        order.setTotal(dto.getTotal());
        order.setDiscount(dto.getDiscount());
        order.setShippingFee(dto.getShippingFee());
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setPaymentStatus(dto.getPaymentStatus());
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : Order.OrderStatus.PENDING);
        order.setShippingAddress(dto.getShippingAddress());
        return order;
    }
    
    private void updateEntityFromDTO(Order order, OrderDTO dto) {
        if (dto.getTotal() != null) order.setTotal(dto.getTotal());
        if (dto.getDiscount() != null) order.setDiscount(dto.getDiscount());
        if (dto.getShippingFee() != null) order.setShippingFee(dto.getShippingFee());
        if (dto.getPaymentMethod() != null) order.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getPaymentStatus() != null) order.setPaymentStatus(dto.getPaymentStatus());
        if (dto.getStatus() != null) order.setStatus(dto.getStatus());
        if (dto.getShippingAddress() != null) order.setShippingAddress(dto.getShippingAddress());
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderTrackDTO track(String orderId, String email, String phone) {
        // Lấy order theo orderId
        var o = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        // (Optional) xác thực người xem nếu có customer
        if (o.getCustomer() != null) {
            if (email != null && !email.isBlank()
                    && !email.equalsIgnoreCase(o.getCustomer().getEmail())) {
                throw new RuntimeException("Email không khớp với đơn");
            }
            if (phone != null && !phone.isBlank()
                    && !phone.equalsIgnoreCase(o.getCustomer().getPhone())) {
                throw new RuntimeException("SĐT không khớp với đơn");
            }
        }

        // Lấy danh sách shipment theo orderId rồi map sang status
        var shipment = shipmentRepository.findByOrderOrderId(orderId);
        var statuses = shipment.map(s -> List.of(s.getStatus().name())).orElse(List.of());

        // Convert LocalDateTime -> Instant
        var created = (o.getCreateAt() == null) ? null
                : o.getCreateAt().atZone(java.time.ZoneId.systemDefault()).toInstant();
        var updated = (o.getUpdateAt() == null) ? null
                : o.getUpdateAt().atZone(java.time.ZoneId.systemDefault()).toInstant();

        return OrderTrackDTO.builder()
                .orderId(o.getOrderId())
                .status(o.getStatus().name())
                .createdAt(created)
                .updatedAt(updated)
                .shippingAddress(o.getShippingAddress())
                .shipmentStatuses(statuses)
                .build();
    }

    @Override
    @Transactional
    public OrderPlacedDTO guestCheckout(CheckoutRequest req, HttpSession session) {
        // 1) Lấy giỏ hàng
        CartDTO cart = cartService.getCart(session);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 2) Subtotal
        double subtotal = cart.getItems().values().stream()
                .mapToDouble(it -> nz(it.getPrice()) * it.getQty())
                .sum();

        // 3) Khuyến mãi & điểm
        int pointsUsed = Math.max(0, java.util.Optional.ofNullable(req.getUsePoints()).orElse(0));
        String code = java.util.Optional.ofNullable(req.getPromoCode()).map(String::trim).orElse("");

        double promoDiscount = 0d;
        if (!code.isBlank()) {
            var now = java.time.LocalDateTime.now();
            var opt = promotionRepository.findByCodeIgnoreCaseAndStatusAndExpireDateAfter(code, 1, now);
            if (opt.isPresent()) {
                var p = opt.get();
                double minVal = toDouble(p.getMinValue());
                double moneyOff = toDouble(p.getDiscount());
                if (subtotal >= minVal && moneyOff > 0) {
                    promoDiscount = moneyOff;
                }
            }
        }

        double pointsDiscount = pointsUsed * 1_000d; // 1 điểm = 1.000đ
        double discount = Math.min(subtotal, promoDiscount + pointsDiscount);

        // 4) Ship fee + total
        double shippingFee = subtotal >= 300_000 ? 0d : 25_000d;
        double total = Math.max(0d, subtotal - discount + shippingFee);

        // 5) Tạo Order
        Order order = new Order();

        // (Optional) nếu đã login thì gắn customer
        Object uidObj = session.getAttribute(AuthController.SESSION_UID);
        if (uidObj instanceof String uid && !uid.isBlank()) {
            customerRepository.findByCustomerIdAndStatus(uid, 1).ifPresent(order::setCustomer);
        } else {
            order.setCustomer(null); // guest
        }

        order.setShippingAddress(req.getShippingAddress());
        order.setPaymentMethod(Order.PaymentMethod.COD); // hoặc từ req nếu FE gửi
        order.setPaymentStatus(0);
        order.setStatus(Order.OrderStatus.PENDING);

        // Lưu giá trị đã làm tròn VND
        order.setDiscount(roundVND(discount));
        order.setShippingFee(roundVND(shippingFee));
        order.setTotal(roundVND(total));

        // 6) Tạo OrderItem từ giỏ
        java.util.List<OrderItem> items = new java.util.ArrayList<>();
        for (CartItemDTO it : cart.getItems().values()) {
            Book book = bookRepository.findById(it.getBookId()).orElseThrow();

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setBook(book);
            oi.setQuantity(it.getQty());
            oi.setPrice(roundVND(nz(it.getPrice()))); // snapshot giá tại thời điểm đặt
            items.add(oi);

            // (Optional) trừ tồn kho
            // Integer cur = book.getQuantity() == null ? 0 : book.getQuantity();
            // book.setQuantity(Math.max(0, cur - it.getQty()));
            // bookRepository.save(book);
        }
        order.setOrderItems(items); // Cascade.ALL sẽ lo lưu

        // 7) Lưu order + clear cart
        orderRepository.save(order);
        cartService.clear(session);

        // 8) Trả về DTO
        return OrderPlacedDTO.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus().name())
                .subtotal(roundVND(subtotal))
                .discount(roundVND(discount))
                .shippingFee(roundVND(shippingFee))
                .total(roundVND(total))
                .pointsUsed(pointsUsed)
                .pointsEarned(0) // nếu có rule tích điểm thì đổi ở đây
                .build();
    }

    private static double toDouble(Object v) {
        if (v == null)
            return 0d;
        if (v instanceof Number n)
            return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0d;
        }
    }
    
    private static double nz(Object v) {
        return toDouble(v);
    }
    
    private static double roundVND(double v) {
        return Math.round(v / 1_000d) * 1_000d;
    }
}