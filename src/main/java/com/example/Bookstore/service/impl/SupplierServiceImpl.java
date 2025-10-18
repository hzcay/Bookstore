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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    public Page<SupplierDTO> getAllSuppliers(String searchTerm, Pageable pageable) {
        Page<Supplier> suppliers;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            suppliers = supplierRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                searchTerm, searchTerm, pageable);
        } else {
            suppliers = supplierRepository.findAll(pageable);
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
            suppliers = supplierRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseAndDebtCondition(
                searchTerm, searchTerm, hasDebt, pageable);
        } else {
            if (hasDebt) {
                suppliers = supplierRepository.findByDebtGreaterThan(0.0, pageable);
            } else {
                suppliers = supplierRepository.findByDebtEquals(0.0, pageable);
            }
        }
        
        return suppliers.map(this::convertToDTO);
    }

    @Override
    public Optional<SupplierDTO> getSupplierById(String id) {
        return supplierRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        Supplier supplier = convertToEntity(supplierDTO);
        supplier.setSupplierId(null);
        Supplier savedSupplier = supplierRepository.save(supplier);
        return convertToDTO(savedSupplier);
    }

    @Override
    public SupplierDTO updateSupplier(String id, SupplierDTO supplierDTO) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        
        supplier.setName(supplierDTO.getName());
        supplier.setPhone(supplierDTO.getPhone());
        supplier.setAddress(supplierDTO.getAddress());
        supplier.setDebt(supplierDTO.getDebt());
        supplier.setStatus(supplierDTO.getStatus());
        
        Supplier updatedSupplier = supplierRepository.save(supplier);
        return convertToDTO(updatedSupplier);
    }

    @Override
    public void deleteSupplier(String id) {
        supplierRepository.deleteById(id);
    }

    @Override
    public void updateSupplierDebt(String id, Double debt) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        supplier.setDebt(debt);
        supplierRepository.save(supplier);
    }

    @Override
    public List<SupplierDTO> getActiveSuppliers() {
        return supplierRepository.findByStatus(1).stream()
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
            ? supplierRepository.findAll(pageable)
            : supplierRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }
    
    @Override
    public Supplier create(String name, String address, String phone) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setAddress(address);
        s.setPhone(phone);
        s.setDebt(0.0);
        s.setStatus(1);
        return supplierRepository.save(s);
    }
    
    @Override
    public Supplier update(String id, String name, String address, String phone) {
        Supplier s = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        s.setName(name);
        s.setAddress(address);
        s.setPhone(phone);
        return supplierRepository.save(s);
    }
    
    @Override
    public void delete(String id) {
        supplierRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Supplier findById(String id) {
        return supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }
}

