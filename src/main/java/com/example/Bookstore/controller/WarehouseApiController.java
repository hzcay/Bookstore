package com.example.Bookstore.controller;

import com.example.Bookstore.dto.warehouse.CreateReceiptRequest;
import com.example.Bookstore.dto.warehouse.InventoryStockDTO;
import com.example.Bookstore.dto.warehouse.LowStockDTO;
import com.example.Bookstore.dto.warehouse.SupplierRequest;
import com.example.Bookstore.entity.Inventory;
import com.example.Bookstore.entity.Supplier;
import com.example.Bookstore.service.InventoryService;
import com.example.Bookstore.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WarehouseApiController {

  private final InventoryService inventoryService;
  private final SupplierService supplierService;

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
}

