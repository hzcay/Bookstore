package com.example.Bookstore.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderTrackDTO {
    private String orderId;
    private String status; // PENDING/PROCESSING/SHIPPED/COMPLETED/CANCELLED
    private Instant createdAt;
    private Instant updatedAt;
    private String shippingAddress;
    private List<String> shipmentStatuses; // nếu có nhiều lần giao
}
