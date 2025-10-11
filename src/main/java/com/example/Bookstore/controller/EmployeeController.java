package com.example.Bookstore.controller;

import com.example.Bookstore.dto.EmployeeDTO;
import com.example.Bookstore.service.EmployeeService;
import com.example.Bookstore.service.AuthService;
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
@RequestMapping("/api/v1/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private AuthService authService;
    
    @GetMapping
    public ResponseEntity<Page<EmployeeDTO>> getAllEmployees(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<EmployeeDTO> employees = employeeService.getAllEmployees(q, pageable);
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable String id) {
        Optional<EmployeeDTO> employee = employeeService.getEmployeeById(id);
        return employee.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        try {
            employeeDTO.setStatus(0);
            EmployeeDTO createdEmployee = employeeService.createEmployee(employeeDTO);
            
            if (createdEmployee.getEmail() != null && !createdEmployee.getEmail().isEmpty()) {
                authService.sendOTP(createdEmployee.getEmail());
            }
            
            return ResponseEntity.ok(createdEmployee);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tạo nhân viên: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable String id, 
                                                      @RequestBody EmployeeDTO employeeDTO) {
        try {
            EmployeeDTO updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
            return ResponseEntity.ok(updatedEmployee);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<EmployeeDTO> getEmployeeByEmail(@PathVariable String email) {
        Optional<EmployeeDTO> employee = employeeService.getEmployeeByEmail(email);
        return employee.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<EmployeeDTO>> getActiveEmployees() {
        List<EmployeeDTO> employees = employeeService.getActiveEmployees();
        return ResponseEntity.ok(employees);
    }

    @PostMapping("/{id}/resend-otp")
    public ResponseEntity<?> resendOTP(@PathVariable String id) {
        try {
            Optional<EmployeeDTO> employee = employeeService.getEmployeeById(id);
            if (employee.isEmpty()) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            if (employee.get().getEmail() == null || employee.get().getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            authService.sendOTP(employee.get().getEmail());
            return ResponseEntity.ok("Đã gửi mã OTP qua email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("OTP không đúng");
        }
    }

    @PostMapping("/{id}/verify-activation")
    public ResponseEntity<?> verifyActivation(@PathVariable String id, @RequestBody VerifyOTPRequest request) {
        try {
            Optional<EmployeeDTO> employee = employeeService.getEmployeeById(id);
            if (employee.isEmpty()) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            if (authService.verifyOTP(employee.get().getEmail(), request.getOtp())) {
                employeeService.activateEmployeeByEmail(employee.get().getEmail());
                return ResponseEntity.ok("Đã xác thực thành công!");
            }
            return ResponseEntity.badRequest().body("OTP không đúng");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("OTP không đúng");
        }
    }

    public static class VerifyOTPRequest {
        private String otp;
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
}

