package com.example.Bookstore.controller;

import com.example.Bookstore.dto.ReportDTO;
import com.example.Bookstore.dto.SalesReportDTO;
import com.example.Bookstore.service.ReportService;
import com.example.Bookstore.service.EmailService;
import com.example.Bookstore.service.AutoReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
@CrossOrigin(origins = "*")
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AutoReportService autoReportService;
    
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
    
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportReportToPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "day") String granularity) {
        
        try {
            byte[] pdfBytes = reportService.exportReportToPDF(fromDate, toDate, granularity);
            
            String filename = "baocao_" + fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                            "_" + toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportReportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "day") String granularity) {
        
        try {
            byte[] excelBytes = reportService.exportReportToExcel(fromDate, toDate, granularity);
            
            String filename = "baocao_" + fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                            "_" + toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API: Phân tích doanh thu theo sách (top 10)
     */
    @GetMapping("/books-revenue")
    public ResponseEntity<?> getBooksRevenueAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            java.util.List<java.util.Map<String, Object>> analysis = 
                reportService.getBooksRevenueAnalysis(from, to);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
    
    // ========== API CẤU HÌNH TỰ ĐỘNG GỬI BÁO CÁO ==========
    
    @PostMapping("/auto-report/configure")
    public ResponseEntity<?> configureAutoReport(@RequestBody java.util.Map<String, Object> config) {
        try {
            String email = (String) config.get("email");
            String time = (String) config.get("time");
            Boolean enabled = (Boolean) config.get("enabled");
            
            if (email == null || time == null || enabled == null) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Thiếu thông tin cấu hình"));
            }
            
            // Gọi AutoReportService để cấu hình
            autoReportService.configureReportSchedule(email, time, enabled);
            
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Đã cấu hình tự động gửi báo cáo",
                "email", email,
                "time", time,
                "enabled", enabled
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
    
    @GetMapping("/auto-report/status")
    public ResponseEntity<?> getAutoReportStatus() {
        try {
            return ResponseEntity.ok(java.util.Map.of(
                "enabled", true,
                "email", "admin@bookstore.com",
                "time", "08:00",
                "nextRun", "Tomorrow at 08:00"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
    
    // ========== TEST EMAIL ==========
    
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String email) {
        try {
            // Gửi email test thật
            emailService.sendSimpleEmail(email, "📧 Test Email từ Bookstore", 
                "Đây là email test từ hệ thống Bookstore.\n" +
                "Nếu bạn nhận được email này, hệ thống gửi email đã hoạt động bình thường.\n\n" +
                "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Test email sent to: " + email,
                "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "Lỗi gửi email: " + e.getMessage()));
        }
    }
    
    // ========== TEST AUTO REPORT ==========
    
    @PostMapping("/test-auto-report")
    public ResponseEntity<?> testAutoReport(@RequestParam String email) {
        try {
            System.out.println("🧪 TEST AUTO REPORT: Starting for email: " + email);
            
            // Test EmailService trực tiếp trước
            System.out.println("🧪 TEST: Testing EmailService directly...");
            emailService.sendSimpleEmail(email, "🧪 TEST AUTO REPORT", 
                "Đây là test auto report từ hệ thống Bookstore.\n" +
                "Nếu bạn nhận được email này, auto report đã hoạt động.\n\n" +
                "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            
            System.out.println("🧪 TEST: Direct email sent, now testing auto report...");
            
            // Cấu hình email và gửi test report ngay
            autoReportService.configureReportSchedule(email, "08:00", true);
            
            // Gọi method test để gửi ngay
            if (autoReportService instanceof com.example.Bookstore.service.impl.AutoReportServiceImpl) {
                System.out.println("🧪 TEST AUTO REPORT: Calling sendTestReport()");
                ((com.example.Bookstore.service.impl.AutoReportServiceImpl) autoReportService).sendTestReport();
            } else {
                System.out.println("❌ TEST AUTO REPORT: AutoReportService is not AutoReportServiceImpl");
            }
            
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Test auto report sent to: " + email,
                "status", "success"
            ));
        } catch (Exception e) {
            System.err.println("❌ TEST AUTO REPORT ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "Lỗi gửi auto report: " + e.getMessage()));
        }
    }
    
    // ========== TEST SCHEDULED TASK ==========
    
    @PostMapping("/test-scheduled")
    public ResponseEntity<?> testScheduled() {
        try {
            System.out.println("🧪 TEST SCHEDULED: Testing scheduled task...");
            
            // Test đơn giản trước
            System.out.println("🧪 TEST SCHEDULED: AutoReportService type: " + autoReportService.getClass().getName());
            
            // Test scheduled task bằng cách gọi method khác
            System.out.println("🧪 TEST SCHEDULED: Testing auto report configuration...");
            autoReportService.configureReportSchedule("test@example.com", "09:30", true);
            
            // Gọi method test thay vì scheduled
            if (autoReportService instanceof com.example.Bookstore.service.impl.AutoReportServiceImpl) {
                System.out.println("🧪 TEST SCHEDULED: Calling sendTestReport() instead");
                try {
                    ((com.example.Bookstore.service.impl.AutoReportServiceImpl) autoReportService).sendTestReport();
                    System.out.println("✅ TEST SCHEDULED: Test report sent successfully");
                } catch (Exception e) {
                    System.err.println("❌ TEST SCHEDULED: Error calling method: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("❌ TEST SCHEDULED: AutoReportService is not AutoReportServiceImpl");
            }
            
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Scheduled task test completed",
                "status", "success"
            ));
        } catch (Exception e) {
            System.err.println("❌ TEST SCHEDULED ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "Lỗi test scheduled: " + e.getMessage()));
        }
    }
}
