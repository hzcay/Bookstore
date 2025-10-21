package com.example.Bookstore.service;

import com.example.Bookstore.dto.SupplierDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Bookstore.dto.SupplierDTO;
import com.example.Bookstore.dto.warehouse.PayDebtRequest;
import java.time.LocalDateTime;
import com.example.Bookstore.entity.PaymentHistory;
import java.util.List;
import java.util.Optional;

public interface SupplierService {
    Page<SupplierDTO> getAllSuppliers(String searchTerm, Pageable pageable);
    Page<SupplierDTO> getAllSuppliers(String searchTerm, Boolean hasDebt, Pageable pageable);
    Optional<SupplierDTO> getSupplierById(String id);
    SupplierDTO createSupplier(SupplierDTO supplierDTO);
    SupplierDTO updateSupplier(String id, SupplierDTO supplierDTO);
    void deleteSupplier(String id);
    void updateSupplierDebt(String id, Double debt);
    List<SupplierDTO> getActiveSuppliers();
    
    // Warehouse support methods
    Page<com.example.Bookstore.entity.Supplier> list(String keyword, Pageable pageable);
    com.example.Bookstore.entity.Supplier create(String name, String address, String phone);
    com.example.Bookstore.entity.Supplier update(String id, String name, String address, String phone);
    void delete(String id);
    List<com.example.Bookstore.entity.Supplier> findAll();
    com.example.Bookstore.entity.Supplier findById(String id);
	
	// Quản lý công nợ
	PaymentHistory payDebt(PayDebtRequest request);
	
	Page<PaymentHistory> getPaymentHistory(String supplierId, Pageable pageable);
	
	Page<PaymentHistory> getPaymentHistoryByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable);
	
	List<PaymentHistory> getAllPaymentHistoryForExport();
}

