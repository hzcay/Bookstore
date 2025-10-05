package com.example.Bookstore.controller;

import com.example.Bookstore.dto.ReportDTO;
import com.example.Bookstore.dto.SalesReportDTO;
import com.example.Bookstore.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
@CrossOrigin(origins = "*")
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    @GetMapping("/sales")
    public ResponseEntity<SalesReportDTO> generateSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "day") String granularity) {
        
        ReportDTO.SalesReport report = reportService.generateSalesReport(from, to, granularity);
        SalesReportDTO out = new SalesReportDTO();
        out.setReportType(report.getReportType());
        out.setFromDate(report.getFromDate());
        out.setToDate(report.getToDate());
        out.setTotalRevenue(report.getTotalRevenue());
        out.setTotalOrders(report.getTotalOrders());
        out.setAdditionalData(report.getAdditionalData());
        return ResponseEntity.ok(out);
    }
    
    @GetMapping("/inventory")
    public ResponseEntity<ReportDTO.InventoryReport> generateInventoryReport() {
        ReportDTO.InventoryReport report = reportService.generateInventoryReport();
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/suppliers-debt")
    public ResponseEntity<ReportDTO.SupplierDebtReport> generateSupplierDebtReport() {
        ReportDTO.SupplierDebtReport report = reportService.generateSupplierDebtReport();
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/revenue")
    public ResponseEntity<Double> calculateTotalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        Double revenue = reportService.calculateTotalRevenue(fromDate, toDate);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/orders-count")
    public ResponseEntity<Long> countTotalOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        Long count = reportService.countTotalOrders(fromDate, toDate);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/average-order-value")
    public ResponseEntity<Double> calculateAverageOrderValue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        Double averageValue = reportService.calculateAverageOrderValue(fromDate, toDate);
        return ResponseEntity.ok(averageValue);
    }
}
