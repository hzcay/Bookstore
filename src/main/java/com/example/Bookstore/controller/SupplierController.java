package com.example.Bookstore.controller;

import com.example.Bookstore.dto.SupplierDTO;
import com.example.Bookstore.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {
    
    @Autowired
    private SupplierService supplierService;
    
    @GetMapping
    public ResponseEntity<Page<SupplierDTO>> getAllSuppliers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SupplierDTO> suppliers = supplierService.getAllSuppliers(q, pageable);
        return ResponseEntity.ok(suppliers);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable String id) {
        Optional<SupplierDTO> supplier = supplierService.getSupplierById(id);
        return supplier.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<SupplierDTO> createSupplier(@RequestBody SupplierDTO supplierDTO) {
        SupplierDTO createdSupplier = supplierService.createSupplier(supplierDTO);
        return ResponseEntity.ok(createdSupplier);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(@PathVariable String id, 
                                                      @RequestBody SupplierDTO supplierDTO) {
        try {
            SupplierDTO updatedSupplier = supplierService.updateSupplier(id, supplierDTO);
            return ResponseEntity.ok(updatedSupplier);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable String id) {
        try {
            supplierService.deleteSupplier(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/debt")
    public ResponseEntity<Void> updateSupplierDebt(@PathVariable String id, 
                                                   @RequestBody DebtUpdateRequest request) {
        try {
            supplierService.updateSupplierDebt(id, request.getDebt());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<SupplierDTO>> getActiveSuppliers() {
        List<SupplierDTO> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(suppliers);
    }
    
    public static class DebtUpdateRequest {
        private Double debt;
        
        public Double getDebt() {
            return debt;
        }
        
        public void setDebt(Double debt) {
            this.debt = debt;
        }
    }
}

