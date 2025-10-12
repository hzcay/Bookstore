package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.ReportDTO;
import com.example.Bookstore.dto.SupplierDTO;
import com.example.Bookstore.entity.Order;
import com.example.Bookstore.entity.Supplier;
import com.example.Bookstore.repository.OrderRepository;
import com.example.Bookstore.repository.SupplierRepository;
import com.example.Bookstore.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;

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
        report.setTotalBooks(0);
        report.setTotalQuantity(0);
        report.setTotalValue(0.0);
        report.setLowStockCount(0);
        return report;
    }

    @Override
    public ReportDTO.SupplierDebtReport generateSupplierDebtReport() {
        List<Supplier> suppliersWithDebt = supplierRepository.findSuppliersWithDebt();
        
        ReportDTO.SupplierDebtReport report = new ReportDTO.SupplierDebtReport();
        report.setReportType("suppliers-debt");
        
        Double totalDebt = suppliersWithDebt.stream()
            .mapToDouble(Supplier::getDebt)
            .sum();
        report.setTotalDebt(totalDebt);
        
        List<SupplierDTO> supplierDTOs = suppliersWithDebt.stream()
            .map(this::convertSupplierToDTO)
            .collect(Collectors.toList());
        report.setSuppliers(supplierDTOs);
        
        return report;
    }
    
    private SupplierDTO convertSupplierToDTO(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO();
        dto.setSupplierId(supplier.getSupplierId());
        dto.setName(supplier.getName());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setDebt(supplier.getDebt());
        dto.setStatus(supplier.getStatus());
        return dto;
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
    
    @Override
    public byte[] exportReportToPDF(LocalDateTime fromDate, LocalDateTime toDate, String granularity) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        document.add(new Paragraph("BÁO CÁO DOANH THU - CÔNG NỢ")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18));
        
        document.add(new Paragraph("Từ ngày: " + fromDate.format(formatter) + 
                                " - Đến ngày: " + toDate.format(formatter))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12));
        
        document.add(new Paragraph(" "));
        
        Double totalRevenue = calculateTotalRevenue(fromDate, toDate);
        Long totalOrders = countTotalOrders(fromDate, toDate);
        Double avgOrderValue = calculateAverageOrderValue(fromDate, toDate);
        
        Table summaryTable = new Table(2);
        summaryTable.addCell("Tổng doanh thu:");
        summaryTable.addCell(String.format("%,.0f VNĐ", totalRevenue));
        summaryTable.addCell("Tổng đơn hàng:");
        summaryTable.addCell(totalOrders.toString());
        summaryTable.addCell("Giá trị TB/đơn:");
        summaryTable.addCell(String.format("%,.0f VNĐ", avgOrderValue));
        
        document.add(summaryTable);
        document.add(new Paragraph(" "));
        
        ReportDTO.SupplierDebtReport debtReport = generateSupplierDebtReport();
        if (debtReport != null && debtReport.getSuppliers() != null && !debtReport.getSuppliers().isEmpty()) {
            document.add(new Paragraph("BÁO CÁO CÔNG NỢ NHÀ CUNG CẤP")
                    .setFontSize(14));
            
            Table debtTable = new Table(4);
            debtTable.addCell("Tên NCC");
            debtTable.addCell("Số điện thoại");
            debtTable.addCell("Địa chỉ");
            debtTable.addCell("Công nợ");
            
            for (SupplierDTO supplier : debtReport.getSuppliers()) {
                debtTable.addCell(supplier.getName());
                debtTable.addCell(supplier.getPhone() != null ? supplier.getPhone() : "N/A");
                debtTable.addCell(supplier.getAddress() != null ? supplier.getAddress() : "N/A");
                debtTable.addCell(String.format("%,.0f VNĐ", supplier.getDebt()));
            }
            
            debtTable.addCell("");
            debtTable.addCell("");
            debtTable.addCell("TỔNG CÔNG NỢ:");
            debtTable.addCell(String.format("%,.0f VNĐ", debtReport.getTotalDebt()));
            
            document.add(debtTable);
        }
        
        document.close();
        return baos.toByteArray();
    }
    
    @Override
    public byte[] exportReportToExcel(LocalDateTime fromDate, LocalDateTime toDate, String granularity) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Báo cáo");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        Row headerRow = sheet.createRow(0);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("BÁO CÁO DOANH THU - CÔNG NỢ");
        
        Row dateRow = sheet.createRow(1);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Từ ngày: " + fromDate.format(formatter) + 
                             " - Đến ngày: " + toDate.format(formatter));
        
        Row emptyRow = sheet.createRow(2);
        
        Double totalRevenue = calculateTotalRevenue(fromDate, toDate);
        Long totalOrders = countTotalOrders(fromDate, toDate);
        Double avgOrderValue = calculateAverageOrderValue(fromDate, toDate);
        
        Row revenueRow = sheet.createRow(3);
        revenueRow.createCell(0).setCellValue("Tổng doanh thu:");
        revenueRow.createCell(1).setCellValue(String.format("%,.0f VNĐ", totalRevenue));
        
        Row ordersRow = sheet.createRow(4);
        ordersRow.createCell(0).setCellValue("Tổng đơn hàng:");
        ordersRow.createCell(1).setCellValue(totalOrders);
        
        Row avgRow = sheet.createRow(5);
        avgRow.createCell(0).setCellValue("Giá trị TB/đơn:");
        avgRow.createCell(1).setCellValue(String.format("%,.0f VNĐ", avgOrderValue));
        
        Row emptyRow2 = sheet.createRow(6);
        
        ReportDTO.SupplierDebtReport debtReport = generateSupplierDebtReport();
        if (debtReport != null && debtReport.getSuppliers() != null && !debtReport.getSuppliers().isEmpty()) {
            Row debtHeaderRow = sheet.createRow(7);
            debtHeaderRow.createCell(0).setCellValue("BÁO CÁO CÔNG NỢ NHÀ CUNG CẤP");
            
            Row debtTableHeader = sheet.createRow(8);
            debtTableHeader.createCell(0).setCellValue("Tên NCC");
            debtTableHeader.createCell(1).setCellValue("Số điện thoại");
            debtTableHeader.createCell(2).setCellValue("Địa chỉ");
            debtTableHeader.createCell(3).setCellValue("Công nợ");
            
            int rowNum = 9;
            for (SupplierDTO supplier : debtReport.getSuppliers()) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(supplier.getName());
                dataRow.createCell(1).setCellValue(supplier.getPhone() != null ? supplier.getPhone() : "N/A");
                dataRow.createCell(2).setCellValue(supplier.getAddress() != null ? supplier.getAddress() : "N/A");
                dataRow.createCell(3).setCellValue(String.format("%,.0f VNĐ", supplier.getDebt()));
            }
            
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("");
            totalRow.createCell(1).setCellValue("");
            totalRow.createCell(2).setCellValue("TỔNG CÔNG NỢ:");
            totalRow.createCell(3).setCellValue(String.format("%,.0f VNĐ", debtReport.getTotalDebt()));
        }
        
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        return baos.toByteArray();
    }
}


