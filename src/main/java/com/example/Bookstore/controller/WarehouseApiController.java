package com.example.Bookstore.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Bookstore.dto.warehouse.CreateReceiptRequest;
import com.example.Bookstore.dto.warehouse.InventoryStockDTO;
import com.example.Bookstore.dto.warehouse.LowStockDTO;
import com.example.Bookstore.dto.warehouse.SupplierRequest;
import com.example.Bookstore.entity.Inventory;
import com.example.Bookstore.entity.Supplier;
import com.example.Bookstore.service.ExcelExportService;
import com.example.Bookstore.service.InventoryService;
import com.example.Bookstore.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyRole('WAREHOUSE','MANAGER')") // Tạm comment để cho phép truy cập public vào warehouse
public class WarehouseApiController {

  private final InventoryService inventoryService;
  private final SupplierService supplierService;
  private final ExcelExportService excelExportService;

  @GetMapping("/stock")
  public Page<InventoryStockDTO> stock(@RequestParam(required=false) String keyword,
                                       @RequestParam(defaultValue="0") int page,
                                       @RequestParam(defaultValue="20") int size) {
    return inventoryService.listStock(keyword, PageRequest.of(page, size));
  }

  @GetMapping("/stock/low")
  public List<LowStockDTO> low(@RequestParam(defaultValue="10") int threshold) {
    return inventoryService.lowStock(threshold);
  }

  @PostMapping("/receipts")
  public Inventory createReceipt(@Valid @RequestBody CreateReceiptRequest req) {
    return inventoryService.createReceipt(req);
  }

  @GetMapping("/receipts")
  public Page<Inventory> receipts(
      @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="20") int size
  ) {
    return inventoryService.listReceipts(from, to, PageRequest.of(page, size));
  }

  @GetMapping("/receipts/export")
  public ResponseEntity<byte[]> exportReceipts(
      @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
  ) throws IOException {
    // Get all receipts without pagination for export
    List<Inventory> receipts = inventoryService.listReceiptsForExport(from, to);
    
    // Build date range label
    DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    String dateRangeLabel;
    if (from != null && to != null) {
      dateRangeLabel = from.format(labelFormatter) + " - " + to.format(labelFormatter);
    } else if (from != null) {
      dateRangeLabel = "Từ " + from.format(labelFormatter);
    } else if (to != null) {
      dateRangeLabel = "Đến " + to.format(labelFormatter);
    } else {
      dateRangeLabel = "Tất cả thời gian";
    }
    
    byte[] excelBytes = excelExportService.exportReceiptsToExcel(receipts, dateRangeLabel);
    
    String filename = "BaoCaoNhapSach_" + 
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(excelBytes.length);
    
    return ResponseEntity.ok()
        .headers(headers)
        .body(excelBytes);
  }

  // Supplier
  @GetMapping("/suppliers")
  public Page<Supplier> suppliers(@RequestParam(required=false) String keyword,
                                  @RequestParam(defaultValue="0") int page,
                                  @RequestParam(defaultValue="20") int size) {
    return supplierService.list(keyword, PageRequest.of(page, size));
  }

  @PostMapping("/suppliers")
  public Supplier createSupplier(@Valid @RequestBody SupplierRequest req) {
    return supplierService.create(req.name(), req.address(), req.phone());
  }

  @PutMapping("/suppliers/{id}")
  public Supplier updateSupplier(@PathVariable String id, @Valid @RequestBody SupplierRequest req) {
    return supplierService.update(id, req.name(), req.address(), req.phone());
  }

  @DeleteMapping("/suppliers/{id}")
  public void deleteSupplier(@PathVariable String id) {
    supplierService.delete(id);
  }
  
  // ===== QUẢN LÝ CÔNG NỢ =====
  
  @GetMapping("/debt-report/export")
  public ResponseEntity<byte[]> exportDebtReport() throws IOException {
    List<com.example.Bookstore.entity.PaymentHistory> paymentHistory = 
      supplierService.getAllPaymentHistoryForExport();
    
    List<Supplier> suppliersWithDebt = supplierService.findAll().stream()
      .filter(s -> s.getDebt() != null && s.getDebt() > 0)
      .toList();
    
    byte[] excelBytes = excelExportService.exportDebtReportToExcel(suppliersWithDebt, paymentHistory);
    
    String filename = "BaoCaoCongNo_" + 
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(excelBytes.length);
    
    return ResponseEntity.ok()
        .headers(headers)
        .body(excelBytes);
  }
}
