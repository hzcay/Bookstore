package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.SupplierDTO;
import com.example.Bookstore.entity.Supplier;
import com.example.Bookstore.repository.SupplierRepository;
import com.example.Bookstore.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import com.example.Bookstore.repository.PaymentHistoryRepository;
import com.example.Bookstore.entity.PaymentHistory;
import com.example.Bookstore.dto.warehouse.PayDebtRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepo;

    @Override
    public Page<SupplierDTO> getAllSuppliers(String searchTerm, Pageable pageable) {
        Page<Supplier> suppliers;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            suppliers = supplierRepo.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                searchTerm, searchTerm, pageable);
        } else {
            suppliers = supplierRepo.findAll(pageable);
        }
        return suppliers.map(this::convertToDTO);
    }

    @Override
    public Page<SupplierDTO> getAllSuppliers(String searchTerm, Boolean hasDebt, Pageable pageable) {
        if (hasDebt == null) {
            return getAllSuppliers(searchTerm, pageable);
        }
        
        Page<Supplier> suppliers;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            suppliers = supplierRepo.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseAndDebtCondition(
                searchTerm, searchTerm, hasDebt, pageable);
        } else {
            if (hasDebt) {
                suppliers = supplierRepo.findByDebtGreaterThan(0.0, pageable);
            } else {
                suppliers = supplierRepo.findByDebtEquals(0.0, pageable);
            }
        }
        
        return suppliers.map(this::convertToDTO);
    }

    @Override
    public Optional<SupplierDTO> getSupplierById(String id) {
        return supplierRepo.findById(id).map(this::convertToDTO);
    }

    @Override
    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        Supplier supplier = convertToEntity(supplierDTO);
        supplier.setSupplierId(null);
        Supplier savedSupplier = supplierRepo.save(supplier);
        return convertToDTO(savedSupplier);
    }

    @Override
    public SupplierDTO updateSupplier(String id, SupplierDTO supplierDTO) {
        Supplier supplier = supplierRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        
        supplier.setName(supplierDTO.getName());
        supplier.setPhone(supplierDTO.getPhone());
        supplier.setAddress(supplierDTO.getAddress());
        supplier.setDebt(supplierDTO.getDebt());
        supplier.setStatus(supplierDTO.getStatus());
        
        Supplier updatedSupplier = supplierRepo.save(supplier);
        return convertToDTO(updatedSupplier);
    }

    @Override
    public void deleteSupplier(String id) {
        supplierRepo.deleteById(id);
    }

    @Override
    public void updateSupplierDebt(String id, Double debt) {
        Supplier supplier = supplierRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        supplier.setDebt(debt);
        supplierRepo.save(supplier);
    }

    @Override
    public List<SupplierDTO> getActiveSuppliers() {
        return supplierRepo.findByStatus(1).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private SupplierDTO convertToDTO(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO();
        dto.setSupplierId(supplier.getSupplierId());
        dto.setName(supplier.getName());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setDebt(supplier.getDebt());
        dto.setStatus(supplier.getStatus());
        return dto;
    }

    private Supplier convertToEntity(SupplierDTO dto) {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(dto.getSupplierId());
        supplier.setName(dto.getName());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setDebt(dto.getDebt() != null ? dto.getDebt() : 0.0);
        supplier.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return supplier;
    }
    
    // ===== Warehouse support methods =====
    @Override
    @Transactional(readOnly = true)
    public Page<Supplier> list(String keyword, Pageable pageable) {
        return (keyword == null || keyword.isBlank())
            ? supplierRepo.findAll(pageable)
            : supplierRepo.findByNameContainingIgnoreCase(keyword, pageable);
    }
    
    @Override
    public Supplier create(String name, String address, String phone) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setAddress(address);
        s.setPhone(phone);
        s.setDebt(0.0);
        s.setStatus(1);
        return supplierRepo.save(s);
    }
    
    @Override
    public Supplier update(String id, String name, String address, String phone) {
        Supplier s = supplierRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        s.setName(name);
        s.setAddress(address);
        s.setPhone(phone);
        return supplierRepo.save(s);
    }
    
    @Override
    public void delete(String id) {
        supplierRepo.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepo.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Supplier findById(String id) {
        return supplierRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }
    
    @Override
  public PaymentHistory payDebt(PayDebtRequest request) {
    Supplier supplier = supplierRepo.findById(request.supplierId())
      .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));
    
    if (request.paymentAmount() <= 0) {
      throw new IllegalArgumentException("Payment amount must be greater than 0");
    }
    
    Double currentDebt = supplier.getDebt() != null ? supplier.getDebt() : 0.0;
    
    if (request.paymentAmount() > currentDebt) {
      throw new IllegalArgumentException("Payment amount cannot exceed current debt: " + currentDebt);
    }
    
    // Tạo lịch sử thanh toán
    PaymentHistory payment = new PaymentHistory();
    payment.setSupplier(supplier);
    payment.setPaymentAmount(request.paymentAmount());
    payment.setPaymentDate(LocalDateTime.now());
    payment.setPaymentMethod(request.paymentMethod());
    payment.setNote(request.note());
    payment.setEmployeeName(request.employeeName());
    payment.setRemainingDebt(currentDebt - request.paymentAmount());
    payment.setStatus(1);
    paymentHistoryRepo.save(payment);
    
    // Cập nhật công nợ NCC
    supplier.setDebt(currentDebt - request.paymentAmount());
    supplierRepo.save(supplier);
    
    return payment;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PaymentHistory> getPaymentHistory(String supplierId, Pageable pageable) {
    return paymentHistoryRepo.findBySupplierId(supplierId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PaymentHistory> getPaymentHistoryByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
    return paymentHistoryRepo.findByDateRange(from, to, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentHistory> getAllPaymentHistoryForExport() {
    return paymentHistoryRepo.findAllForExport();
  }
}

