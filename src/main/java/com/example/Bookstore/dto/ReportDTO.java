package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    
    private String reportType;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Double totalRevenue;
    private Long totalOrders;
    private Map<String, Object> additionalData;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesReport extends ReportDTO {
        private Map<String, Double> revenueByPeriod;
        private Map<String, Long> ordersByPeriod;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryReport extends ReportDTO {
        private Integer totalBooks;
        private Integer totalQuantity;
        private Double totalValue;
        private Integer lowStockCount;
        private Map<String, Integer> stockByCategory;
        private Map<String, Integer> stockByAuthor;
        private Map<String, Integer> stockByPublisher;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDebtReport extends ReportDTO {
        private Double totalDebt;
        private Map<String, Double> debtBySupplier;
        private java.util.List<SupplierDTO> suppliers;
    }
}
