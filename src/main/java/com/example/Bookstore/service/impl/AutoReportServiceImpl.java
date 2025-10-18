package com.example.Bookstore.service.impl;

import com.example.Bookstore.service.AutoReportService;
import com.example.Bookstore.service.ReportService;
import com.example.Bookstore.service.InvoiceService;
import com.example.Bookstore.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoReportServiceImpl implements AutoReportService {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private InvoiceService invoiceService;
    
    @Autowired
    private EmailService emailService;
    
    // Cấu hình mặc định
    private String adminEmail = "bacdoan52@gmail.com";
    private LocalTime reportTime = LocalTime.of(8, 0); // 8:00 sáng
    private boolean enabled = true;
    private LocalDate lastSentDate = null; // Tránh gửi nhiều lần trong ngày
    
    /**
     * Kiểm tra và gửi báo cáo tự động mỗi phút
     * Sẽ gửi khi đúng giờ admin đã cài đặt
     */
    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút
    public void checkAndSendReport() {
        // Log mỗi phút để debug
        System.out.println("⏰ Scheduled task running at: " + LocalDateTime.now() + 
                         " | Enabled: " + enabled + 
                         " | Target time: " + reportTime);
        
        if (!enabled) {
            return; // Không log nếu disabled
        }
        
        LocalTime now = LocalTime.now();
        LocalTime targetTime = reportTime;
        LocalDate today = LocalDate.now();
        
        System.out.println("🔍 Checking time: " + now + " vs target: " + targetTime);
        
        // Kiểm tra nếu đúng giờ (chỉ so sánh giờ và phút)
        if (now.getHour() == targetTime.getHour() && now.getMinute() == targetTime.getMinute()) {
            // Tránh gửi nhiều lần trong ngày
            if (lastSentDate == null || !lastSentDate.equals(today)) {
                System.out.println("🕐 Auto report time reached: " + now + " (target: " + targetTime + ")");
                sendScheduledReport();
                lastSentDate = today; // Đánh dấu đã gửi hôm nay
            } else {
                System.out.println("📧 Report already sent today: " + today);
            }
        } else {
            System.out.println("⏳ Not time yet. Current: " + now + ", Target: " + targetTime);
        }
    }
    
    /**
     * Gửi báo cáo theo lịch trình
     */
    @Override
    public void sendScheduledReport() {
        System.out.println("📧 Sending scheduled report to: " + adminEmail);
        if (!enabled) {
            System.out.println("📧 Auto report is disabled");
            return;
        }
        
        try {
            // Sử dụng thời gian hiện tại (không trừ 1 ngày)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fromDate = now.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime toDate = now.withHour(23).withMinute(59).withSecond(59);
            
            System.out.println("📅 Report period: " + fromDate + " to " + toDate);
            
            sendDailyReportToAdmin(adminEmail, fromDate, toDate);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending auto report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gửi báo cáo PDF cho admin
     */
    @Override
    public void sendDailyReportToAdmin(String adminEmail, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            // Lấy dữ liệu báo cáo
            Double totalRevenue = reportService.calculateTotalRevenue(fromDate, toDate);
            Long totalOrders = reportService.countTotalOrders(fromDate, toDate);
            Double avgOrderValue = reportService.calculateAverageOrderValue(fromDate, toDate);
            
            // Tạo PDF báo cáo
            String html = generateReportHTML(fromDate, toDate, totalRevenue, totalOrders, avgOrderValue);
            byte[] pdfBytes = invoiceService.generatePDFFromHTML(html);
            
            // Gửi email với PDF đính kèm
            String emailSubject = "📊 Báo cáo doanh thu ngày " + fromDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String emailText = String.format("""
                <h2>📊 Báo cáo doanh thu hàng ngày</h2>
                <p><strong>Thời gian:</strong> %s đến %s</p>
                <p><strong>Tổng doanh thu:</strong> %,.0f đ</p>
                <p><strong>Tổng đơn hàng:</strong> %d</p>
                <p><strong>Giá trị trung bình/đơn:</strong> %,.0f đ</p>
                <p>Xem chi tiết trong file PDF đính kèm.</p>
                """, 
                fromDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                toDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                totalRevenue != null ? totalRevenue : 0.0,
                totalOrders != null ? totalOrders : 0,
                avgOrderValue != null ? avgOrderValue : 0.0
            );
            
            emailService.sendEmailWithAttachment(
                adminEmail, 
                emailSubject, 
                emailText, 
                pdfBytes, 
                "bao-cao-doanh-thu-" + fromDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf"
            );
            
            System.out.println("✅ Daily report sent successfully to: " + adminEmail);
            System.out.println("📊 Revenue: " + totalRevenue + ", Orders: " + totalOrders);
            
        } catch (Exception e) {
            System.err.println("❌ Error generating daily report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cấu hình thời gian gửi báo cáo
     */
    @Override
    public void configureReportSchedule(String email, String time, boolean enabled) {
        System.out.println("🔧 CONFIGURE: Before - Email: " + this.adminEmail + ", Time: " + this.reportTime + ", Enabled: " + this.enabled);
        
        this.adminEmail = email;
        this.reportTime = LocalTime.parse(time);
        this.enabled = enabled;
        
        System.out.println("⚙️ Auto report configured:");
        System.out.println("📧 Email: " + email);
        System.out.println("⏰ Time: " + time);
        System.out.println("🔛 Enabled: " + enabled);
        
        System.out.println("🔧 CONFIGURE: After - Email: " + this.adminEmail + ", Time: " + this.reportTime + ", Enabled: " + this.enabled);
    }
    
    /**
     * TEST: Gửi báo cáo ngay lập tức (không chờ scheduled)
     */
    public void sendTestReport() {
        System.out.println("🧪 TEST: Sending report immediately...");
        System.out.println("🧪 TEST: Admin email: " + adminEmail);
        System.out.println("🧪 TEST: Enabled: " + enabled);
        
        try {
            // Sử dụng thời gian hiện tại (không trừ 1 ngày)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fromDate = now.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime toDate = now.withHour(23).withMinute(59).withSecond(59);
            
            System.out.println("📧 TEST: Sending daily report to: " + adminEmail);
            System.out.println("📅 TEST: Report period: " + fromDate + " to " + toDate);
            
            // Gọi method gửi báo cáo
            sendDailyReportToAdmin(adminEmail, fromDate, toDate);
            
            System.out.println("✅ TEST: Report sent successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ TEST: Error sending auto report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tạo HTML cho báo cáo
     */
    private String generateReportHTML(LocalDateTime fromDate, LocalDateTime toDate, 
                                    Double totalRevenue, Long totalOrders, Double avgOrderValue) {
        return String.format("""
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta charset="UTF-8" />
                <title>Bao cao doanh thu hang ngay</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
                    .header { text-align: center; color: #333; margin-bottom: 30px; }
                    .card { border: 1px solid #ddd; margin: 20px 0; padding: 20px; border-radius: 5px; }
                    .card-header { background: #f8f9fa; padding: 10px; margin: -20px -20px 20px -20px; border-radius: 5px 5px 0 0; }
                    .metric-row { display: flex; justify-content: space-between; margin: 10px 0; }
                    .metric-label { font-weight: bold; color: #666; }
                    .metric-value { font-weight: bold; color: #333; }
                    .table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .table th, .table td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                    .table th { background: #f8f9fa; font-weight: bold; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>BAO CAO DOANH THU HANG NGAY</h1>
                    <p>Thoi gian: %s den %s</p>
                    <p>Bao cao duoc tao tu dong luc: %s</p>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h3>Tong quan doanh thu</h3>
                    </div>
                    <div class="metric-row">
                        <span class="metric-label">Tong doanh thu:</span>
                        <span class="metric-value">%,.0f d</span>
                    </div>
                    <div class="metric-row">
                        <span class="metric-label">Tong don hang:</span>
                        <span class="metric-value">%d</span>
                    </div>
                    <div class="metric-row">
                        <span class="metric-label">Gia tri TB/don:</span>
                        <span class="metric-value">%,.0f d</span>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h3>Bao cao ton kho</h3>
                    </div>
                    <table class="table">
                        <tr>
                            <th>Tong so loai sach:</th>
                            <td>%d</td>
                        </tr>
                        <tr>
                            <th>Tong so luong:</th>
                            <td>%d</td>
                        </tr>
                        <tr>
                            <th>Gia tri kho:</th>
                            <td>%,.0f d</td>
                        </tr>
                        <tr>
                            <th>So sach sap het:</th>
                            <td>%d</td>
                        </tr>
                    </table>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h3>Phan tich doanh thu theo sach</h3>
                    </div>
                    <table class="table">
                        <tr>
                            <th>STT</th>
                            <th>Ten sach</th>
                            <th>Doanh thu</th>
                            <th>So luong ban</th>
                            <th>Ty le</th>
                        </tr>
                        %s
                    </table>
                </div>
                
                <div class="footer">
                    <p>He thong Bookstore - Bao cao tu dong</p>
                </div>
            </body>
            </html>
            """, 
            fromDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            toDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
            totalRevenue != null ? totalRevenue : 0.0,
            totalOrders != null ? totalOrders : 0,
            avgOrderValue != null ? avgOrderValue : 0.0,
            getInventoryTotalBooks(),
            getInventoryTotalQuantity(),
            getInventoryTotalValue(),
            getInventoryLowStockCount(),
            generateBooksRevenueTable(fromDate, toDate)
        );
    }
    
    /**
     * Lấy dữ liệu tồn kho cho PDF
     */
    private int getInventoryTotalBooks() {
        try {
            return reportService.generateInventoryReport().getTotalBooks();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getInventoryTotalQuantity() {
        try {
            return reportService.generateInventoryReport().getTotalQuantity();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double getInventoryTotalValue() {
        try {
            return reportService.generateInventoryReport().getTotalValue();
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private int getInventoryLowStockCount() {
        try {
            return reportService.generateInventoryReport().getLowStockCount();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Tạo bảng phân tích doanh thu theo sách
     */
    private String generateBooksRevenueTable(LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            List<java.util.Map<String, Object>> booksAnalysis = reportService.getBooksRevenueAnalysis(fromDate, toDate);
            
            StringBuilder tableRows = new StringBuilder();
            int stt = 1;
            
            for (java.util.Map<String, Object> book : booksAnalysis) {
                String title = (String) book.get("title");
                Double revenue = (Double) book.get("revenue");
                Integer quantity = (Integer) book.get("quantity");
                Double percentage = (Double) book.get("percentage");
                
                tableRows.append(String.format("""
                    <tr>
                        <td>%d</td>
                        <td>%s</td>
                        <td>%,.0f d</td>
                        <td>%d</td>
                        <td>%.2f%%</td>
                    </tr>
                    """, 
                    stt++,
                    title != null ? title : "Unknown",
                    revenue != null ? revenue : 0.0,
                    quantity != null ? quantity : 0,
                    percentage != null ? percentage : 0.0
                ));
            }
            
            return tableRows.toString();
            
        } catch (Exception e) {
            System.err.println("❌ Error generating books revenue table: " + e.getMessage());
            return "<tr><td colspan='5'>Khong co du lieu</td></tr>";
        }
    }
}
