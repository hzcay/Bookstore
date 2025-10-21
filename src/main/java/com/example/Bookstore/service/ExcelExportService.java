package com.example.Bookstore.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.Bookstore.entity.Inventory;
import com.example.Bookstore.entity.PaymentHistory;
import com.example.Bookstore.entity.Supplier;

@Service
public class ExcelExportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat VN_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    public byte[] exportReceiptsToExcel(List<Inventory> receipts, String dateRangeLabel) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Bao cao nhap sach");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);
        CellStyle summaryLabelStyle = createBoldStyle(workbook);
        CellStyle summaryValueStyle = createBoldNumberStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📋 BÁO CÁO CHI TIẾT PHIẾU NHẬP KHO");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        titleRow.setHeightInPoints(35);
        
        // Subtitle
        Row subtitleRow = sheet.createRow(rowNum++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("🏪 Hệ thống quản lý nhà sách");
        subtitleCell.setCellStyle(createSubtitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
        
        // Date range
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("📅 Khoảng thời gian: " + (dateRangeLabel != null ? dateRangeLabel : "Tất cả"));
        dateCell.setCellStyle(createSubtitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));
        
        // Export time
        Row exportRow = sheet.createRow(rowNum++);
        Cell exportCell = exportRow.createCell(0);
        exportCell.setCellValue("🕐 Ngày xuất báo cáo: " + java.time.LocalDateTime.now().format(DATE_FORMATTER));
        exportCell.setCellStyle(createSubtitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 7));
        
        rowNum++; // Empty row
        
        // SECTION 1: TỔNG HỢP THỐNG KÊ
        Row statHeaderRow = sheet.createRow(rowNum++);
        Cell statHeaderCell = statHeaderRow.createCell(0);
        statHeaderCell.setCellValue("📊 THÔNG TIN TỔNG HỢP");
        statHeaderCell.setCellStyle(createSectionHeaderStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));
        statHeaderRow.setHeightInPoints(25);
        
        // Calculate statistics
        int totalReceipts = receipts.size();
        int totalQuantity = receipts.stream().mapToInt(Inventory::getQuantity).sum();
        double totalAmount = receipts.stream()
            .mapToDouble(r -> r.getTotalAmount() != null ? r.getTotalAmount() : 0.0)
            .sum();
        double avgImportPrice = receipts.isEmpty() ? 0 : 
            receipts.stream()
                .mapToDouble(r -> r.getImportPrice() != null ? r.getImportPrice() : 0.0)
                .average()
                .orElse(0.0);
        long uniqueBooks = receipts.stream()
            .map(r -> r.getBook().getBookId())
            .distinct()
            .count();
        long uniqueSuppliers = receipts.stream()
            .map(r -> r.getSupplier().getSupplierId())
            .distinct()
            .count();
        
        // Statistics rows
        rowNum = createStatRow(sheet, rowNum, "Tổng số phiếu nhập:", totalReceipts + " phiếu", summaryLabelStyle, summaryValueStyle);
        rowNum = createStatRow(sheet, rowNum, "Tổng số lượng nhập:", VN_FORMAT.format(totalQuantity) + " cuốn", summaryLabelStyle, summaryValueStyle);
        rowNum = createStatRow(sheet, rowNum, "Tổng giá trị nhập:", VN_FORMAT.format(totalAmount) + " VND", summaryLabelStyle, summaryValueStyle);
        rowNum = createStatRow(sheet, rowNum, "Giá nhập trung bình:", VN_FORMAT.format(avgImportPrice) + " VND/cuốn", summaryLabelStyle, summaryValueStyle);
        rowNum = createStatRow(sheet, rowNum, "Số đầu sách đã nhập:", uniqueBooks + " đầu sách", summaryLabelStyle, summaryValueStyle);
        rowNum = createStatRow(sheet, rowNum, "Số nhà cung cấp:", uniqueSuppliers + " NCC", summaryLabelStyle, summaryValueStyle);
        
        rowNum++; // Empty row
        
        // SECTION 2: CHI TIẾT PHIẾU NHẬP
        Row detailHeaderRow = sheet.createRow(rowNum++);
        Cell detailHeaderCell = detailHeaderRow.createCell(0);
        detailHeaderCell.setCellValue("📦 CHI TIẾT CÁC PHIẾU NHẬP");
        detailHeaderCell.setCellStyle(createSectionHeaderStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));
        detailHeaderRow.setHeightInPoints(25);
        
        // Table header
        Row tableHeaderRow = sheet.createRow(rowNum++);
        String[] columns = {"STT", "Mã phiếu", "Ngày nhập", "Tên sách", "Nhà cung cấp", "Số lượng", "Giá nhập/cuốn", "Thành tiền"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = tableHeaderRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        tableHeaderRow.setHeightInPoints(25);
        
        // Data rows
        int stt = 1;
        for (Inventory receipt : receipts) {
            Row row = sheet.createRow(rowNum++);
            
            // STT
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(stt++);
            cell0.setCellStyle(numberStyle);
            
            // Mã phiếu
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(receipt.getInventoryId());
            cell1.setCellStyle(dataStyle);
            
            // Ngày nhập
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(receipt.getImportDate() != null ? receipt.getImportDate().format(DATE_FORMATTER) : "");
            cell2.setCellStyle(dataStyle);
            
            // Tên sách
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(receipt.getBook() != null ? receipt.getBook().getTitle() : "");
            cell3.setCellStyle(dataStyle);
            
            // Nhà cung cấp
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(receipt.getSupplier() != null ? receipt.getSupplier().getName() : "");
            cell4.setCellStyle(dataStyle);
            
            // Số lượng
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(receipt.getQuantity());
            cell5.setCellStyle(numberStyle);
            
            // Giá nhập/cuốn
            Cell cell6 = row.createCell(6);
            double importPrice = receipt.getImportPrice() != null ? receipt.getImportPrice() : 0.0;
            cell6.setCellValue(VN_FORMAT.format(importPrice) + " ₫");
            cell6.setCellStyle(moneyStyle);
            
            // Thành tiền
            Cell cell7 = row.createCell(7);
            double total = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : 0.0;
            cell7.setCellValue(VN_FORMAT.format(total) + " ₫");
            cell7.setCellStyle(moneyStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
        
        // Write to output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }
    
    private int createStatRow(Sheet sheet, int rowNum, String label, String value, 
                             CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum++);
        
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));
        
        Cell valueCell = row.createCell(4);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 4, 7));
        
        return rowNum;
    }
    
    private CellStyle createSectionHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBoldNumberStyle(Workbook workbook) {
        CellStyle style = createBoldStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }
    
    // ===== EXPORT BÁO CÁO CÔNG NỢ =====
    
    public byte[] exportDebtReportToExcel(List<Supplier> suppliersWithDebt, List<PaymentHistory> paymentHistory) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Sheet 1: Danh sách công nợ
        Sheet debtSheet = workbook.createSheet("Cong no NCC");
        createDebtSheet(debtSheet, suppliersWithDebt, workbook);
        
        // Sheet 2: Lịch sử thanh toán
        Sheet historySheet = workbook.createSheet("Lich su thanh toan");
        createPaymentHistorySheet(historySheet, paymentHistory, workbook);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }
    
    private void createDebtSheet(Sheet sheet, List<Supplier> suppliers, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BAO CAO CONG NO NHA CUNG CAP");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        titleRow.setHeightInPoints(30);
        
        rowNum++;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] columns = {"Ma NCC", "Ten NCC", "So dien thoai", "Cong no (VND)"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        double totalDebt = 0.0;
        for (Supplier s : suppliers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(s.getSupplierId());
            row.getCell(0).setCellStyle(dataStyle);
            
            row.createCell(1).setCellValue(s.getName());
            row.getCell(1).setCellStyle(dataStyle);
            
            row.createCell(2).setCellValue(s.getPhone() != null ? s.getPhone() : "");
            row.getCell(2).setCellStyle(dataStyle);
            
            double debt = s.getDebt() != null ? s.getDebt() : 0.0;
            row.createCell(3).setCellValue(VN_FORMAT.format(debt));
            row.getCell(3).setCellStyle(moneyStyle);
            
            totalDebt += debt;
        }
        
        // Summary
        rowNum++;
        Row summaryRow = sheet.createRow(rowNum++);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("TONG CONG NO:");
        summaryLabelCell.setCellStyle(createBoldStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));
        
        Cell summaryValueCell = summaryRow.createCell(3);
        summaryValueCell.setCellValue(VN_FORMAT.format(totalDebt));
        summaryValueCell.setCellStyle(createBoldNumberStyle(workbook));
        
        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }
    
    private void createPaymentHistorySheet(Sheet sheet, List<PaymentHistory> payments, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("LICH SU THANH TOAN CONG NO");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        titleRow.setHeightInPoints(30);
        
        rowNum++;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] columns = {"Ngay thanh toan", "NCC", "So tien", "Hinh thuc", "Nhan vien", "Ghi chu", "Con lai"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        double totalPaid = 0.0;
        for (PaymentHistory p : payments) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FORMATTER) : "");
            row.getCell(0).setCellStyle(dataStyle);
            
            row.createCell(1).setCellValue(p.getSupplier() != null ? p.getSupplier().getName() : "");
            row.getCell(1).setCellStyle(dataStyle);
            
            row.createCell(2).setCellValue(VN_FORMAT.format(p.getPaymentAmount()));
            row.getCell(2).setCellStyle(moneyStyle);
            
            row.createCell(3).setCellValue(p.getPaymentMethod() != null ? p.getPaymentMethod() : "");
            row.getCell(3).setCellStyle(dataStyle);
            
            row.createCell(4).setCellValue(p.getEmployeeName() != null ? p.getEmployeeName() : "");
            row.getCell(4).setCellStyle(dataStyle);
            
            row.createCell(5).setCellValue(p.getNote() != null ? p.getNote() : "");
            row.getCell(5).setCellStyle(dataStyle);
            
            row.createCell(6).setCellValue(VN_FORMAT.format(p.getRemainingDebt()));
            row.getCell(6).setCellStyle(moneyStyle);
            
            totalPaid += p.getPaymentAmount();
        }
        
        // Summary
        rowNum++;
        Row summaryRow = sheet.createRow(rowNum++);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("TONG DA TRA:");
        summaryLabelCell.setCellStyle(createBoldStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));
        
        Cell summaryValueCell = summaryRow.createCell(2);
        summaryValueCell.setCellValue(VN_FORMAT.format(totalPaid));
        summaryValueCell.setCellStyle(createBoldNumberStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 2, 6));
        
        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }
    
    private CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        return style;
    }
}
