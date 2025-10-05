package com.example.Bookstore.service;

import com.example.Bookstore.dto.ReportDTO;

import java.time.LocalDateTime;

public interface ReportService {
    
    ReportDTO.SalesReport generateSalesReport(LocalDateTime fromDate, LocalDateTime toDate, String granularity);
    
    ReportDTO.InventoryReport generateInventoryReport();
    
    ReportDTO.SupplierDebtReport generateSupplierDebtReport();
    
    Double calculateTotalRevenue(LocalDateTime fromDate, LocalDateTime toDate);
    
    Long countTotalOrders(LocalDateTime fromDate, LocalDateTime toDate);
    
    Double calculateAverageOrderValue(LocalDateTime fromDate, LocalDateTime toDate);
}
