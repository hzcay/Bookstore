package com.example.Bookstore.service.impl;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Bookstore.dto.warehouse.CreateReceiptRequest;
import com.example.Bookstore.dto.warehouse.InventoryStockDTO;
import com.example.Bookstore.dto.warehouse.LowStockDTO;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.entity.Inventory;
import com.example.Bookstore.entity.Supplier;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.InventoryRepository;
import com.example.Bookstore.repository.SupplierRepository;
import com.example.Bookstore.service.InventoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

  private final InventoryRepository inventoryRepo;
  private final BookRepository bookRepo;
  private final SupplierRepository supplierRepo;

  @Override
  @Transactional(readOnly = true)
  public Page<Inventory> listReceipts(LocalDateTime from, LocalDateTime to, Pageable pageable) {
    // Use native query to avoid IS NULL issues with PostgreSQL
    return inventoryRepo.findByDateRangeNative(from, to, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InventoryStockDTO> listStock(String keyword, Pageable pageable) {
    return inventoryRepo.getCurrentStock(keyword, pageable)
      .map(v -> new InventoryStockDTO(v.getBookId(), v.getTitle(), v.getStock()));
  }

  @Override
  public Inventory createReceipt(CreateReceiptRequest req) {
    Book book = bookRepo.findById(req.bookId())
      .orElseThrow(() -> new IllegalArgumentException("Book not found: " + req.bookId()));
    Supplier supplier = supplierRepo.findById(req.supplierId())
      .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));
    if (req.quantity() <= 0) throw new IllegalArgumentException("Quantity must be > 0");

    Inventory inv = new Inventory();
    inv.setBook(book);
    inv.setSupplier(supplier);
    inv.setQuantity(req.quantity());              // nhập: dương; xuất: âm (nếu có)
    inv.setImportPrice(req.importPrice());
    inv.setTotalAmount(req.quantity() * req.importPrice());
    inv.setImportDate(LocalDateTime.now());
    inv.setStatus(1);
    inventoryRepo.save(inv);

    // Cập nhật nợ cho nhà cung cấp
    double totalAmount = req.quantity() * req.importPrice();
    double currentDebt = supplier.getDebt() != null ? supplier.getDebt() : 0.0;
    supplier.setDebt(currentDebt + totalAmount);
    supplierRepo.save(supplier);

    // Nếu bảng Book có cột quantity (theo báo cáo có), cập nhật tồn ngay:
    if (book.getQuantity() != null) {
      book.setQuantity(book.getQuantity() + req.quantity());
      bookRepo.save(book);
    }

    return inv;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LowStockDTO> lowStock(Integer threshold) {
    int th = (threshold == null ? 10 : threshold);
    Page<InventoryStockDTO> page = listStock(null, PageRequest.of(0, 5000));
    return page.getContent().stream()
      .filter(s -> s.stock() != null && s.stock() <= th)
      .map(s -> new LowStockDTO(s.bookId(), s.title(), s.stock()))
      .toList();
  }
  
  
  @Override
  @Transactional(readOnly = true)
  public StockPageVM listStockView(String keyword, String categoryId, Pageable pageable) {
    var page = bookRepo.pageStockAgg(keyword, categoryId, pageable);

    NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

    var rows = page.getContent().stream().map(r -> {
      String priceFmt = (r.getPrice() == null) ? "—" : nf.format(r.getPrice()) + " VND";
      long stock = r.getStock() == null ? 0L : r.getStock().longValue();
      return new StockRowVM(
          r.getBookId(),
          emptyToDash(r.getTitle()),
          "—", // không có isbn trong Book.java → để gạch ngang
          emptyToDash(r.getAuthorName()),
          emptyToDash(r.getCategoryName()),
          stock,
          priceFmt
      );
    }).toList();

    long totalBooks    = bookRepo.countFiltered(keyword, categoryId);
    long totalQuantity = safe(bookRepo.sumQuantityFiltered(keyword, categoryId));
    long lowStockCount = rows.stream().filter(x -> x.stock() <= 10).count();

    long inventoryValue = page.getContent().stream()
        .mapToLong(a -> (a.getStock() == null ? 0L : a.getStock())
                     * (a.getPrice() == null ? 0L : a.getPrice().longValue()))
        .sum();
    String inventoryValueFmt = nf.format(inventoryValue) + " VND";

    return new StockPageVM(rows, page.isLast(), totalBooks, totalQuantity, lowStockCount, inventoryValueFmt);
  }

  private static String emptyToDash(String s) {
    return (s == null || s.isBlank()) ? "—" : s;
  }
  private static long safe(Long v) { return v == null ? 0L : v; }

  @Override
  @Transactional(readOnly = true)
  public List<Inventory> listReceiptsForExport(LocalDateTime from, LocalDateTime to) {
    // Get all receipts without pagination for Excel export
    return inventoryRepo.findByDateRangeNative(from, to, Pageable.unpaged()).getContent();
  }



}

