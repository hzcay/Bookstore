package com.example.Bookstore.service;

import com.example.Bookstore.dto.warehouse.CreateReceiptRequest;
import com.example.Bookstore.dto.warehouse.InventoryStockDTO;
import com.example.Bookstore.dto.warehouse.LowStockDTO;
import com.example.Bookstore.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryService {
	Page<Inventory> listReceipts(LocalDateTime from, LocalDateTime to, Pageable pageable);

	Page<InventoryStockDTO> listStock(String keyword, Pageable pageable);

	Inventory createReceipt(CreateReceiptRequest req);

	List<LowStockDTO> lowStock(Integer threshold);

	record StockRowVM(String bookId, String title, String isbn, String authorName, String categoryName, long stock,
			String priceFmt) {
	}

	record StockPageVM(java.util.List<StockRowVM> content, boolean last, long totalBooks, long totalQuantity,
			long lowStockCount, String inventoryValueFmt) {
	}

	StockPageVM listStockView(String keyword, String categoryId, Pageable pageable);

}

