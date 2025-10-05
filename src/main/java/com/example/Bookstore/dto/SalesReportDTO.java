package com.example.Bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDTO {
    private String reportType;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Double totalRevenue;
    private Long totalOrders;
    private Map<String, Object> additionalData;
    private Map<String, Double> revenueByPeriod;
    private Map<String, Long> ordersByPeriod;
}


