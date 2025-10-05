package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.ReportDTO;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.repository.OrderRepository;
import com.example.Bookstore.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public ReportDTO.SalesReport generateSalesReport(LocalDateTime fromDate, LocalDateTime toDate, String granularity) {
        ReportDTO.SalesReport report = new ReportDTO.SalesReport();
        report.setReportType("sales");
        report.setFromDate(fromDate);
        report.setToDate(toDate);
        report.setTotalRevenue(calculateTotalRevenue(fromDate, toDate));
        report.setTotalOrders(countTotalOrders(fromDate, toDate));
        Map<String, Object> add = new HashMap<>();
        add.put("granularity", granularity);
        report.setAdditionalData(add);
        return report;
    }

    @Override
    public ReportDTO.InventoryReport generateInventoryReport() {
        ReportDTO.InventoryReport report = new ReportDTO.InventoryReport();
        report.setReportType("inventory");
        return report;
    }

    @Override
    public ReportDTO.SupplierDebtReport generateSupplierDebtReport() {
        ReportDTO.SupplierDebtReport report = new ReportDTO.SupplierDebtReport();
        report.setReportType("suppliers-debt");
        return report;
    }

    @Override
    public Double calculateTotalRevenue(LocalDateTime fromDate, LocalDateTime toDate) {
        Double val = orderRepository.calculateRevenue(fromDate, toDate);
        return val == null ? 0.0 : val;
    }

    @Override
    public Long countTotalOrders(LocalDateTime fromDate, LocalDateTime toDate) {
        Long val = orderRepository.countDeliveredOrders(fromDate, toDate);
        return val == null ? 0L : val;
    }

    @Override
    public Double calculateAverageOrderValue(LocalDateTime fromDate, LocalDateTime toDate) {
        long count = countTotalOrders(fromDate, toDate);
        if (count == 0) return 0.0;
        return calculateTotalRevenue(fromDate, toDate) / count;
    }
}


