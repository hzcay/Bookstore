package com.example.Bookstore.service;

import java.time.LocalDateTime;

public interface AutoReportService {
    
    /**
     * Gửi báo cáo tự động theo lịch
     */
    void sendScheduledReport();
    
    /**
     * Gửi báo cáo PDF cho admin
     */
    void sendDailyReportToAdmin(String adminEmail, LocalDateTime fromDate, LocalDateTime toDate);
    
    /**
     * Cấu hình thời gian gửi báo cáo
     */
    void configureReportSchedule(String email, String time, boolean enabled);
}
