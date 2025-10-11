package com.example.Bookstore.controller;

import com.example.Bookstore.dto.CustomerDTO;
import com.example.Bookstore.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Bookstore.service.AuthService;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "*")
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private AuthService authService;
    
    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> getAllCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        String searchTerm = q != null ? q : 
                           phone != null ? phone : 
                           email != null ? email : null;
        
        Page<CustomerDTO> customers = customerService.getAllCustomers(searchTerm, pageable);
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable String id) {
        Optional<CustomerDTO> customer = customerService.getCustomerById(id);
        return customer.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CustomerDTO customerDTO) {
        try {
            customerDTO.setStatus(0);
            CustomerDTO createdCustomer = customerService.createCustomer(customerDTO);
            
            if (createdCustomer.getEmail() != null && !createdCustomer.getEmail().isEmpty()) {
                authService.sendOTP(createdCustomer.getEmail());
            }
            
            return ResponseEntity.ok(createdCustomer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tạo khách hàng: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable String id, 
                                                      @RequestBody CustomerDTO customerDTO) {
        try {
            CustomerDTO updatedCustomer = customerService.updateCustomer(id, customerDTO);
            return ResponseEntity.ok(updatedCustomer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.out.println("Delete error: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/phone/{phone}")
    public ResponseEntity<CustomerDTO> getCustomerByPhone(@PathVariable String phone) {
        Optional<CustomerDTO> customer = customerService.getCustomerByPhone(phone);
        return customer.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerDTO> getCustomerByEmail(@PathVariable String email) {
        Optional<CustomerDTO> customer = customerService.getCustomerByEmail(email);
        return customer.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/points")
    public ResponseEntity<Void> updateCustomerPoints(@PathVariable String id, 
                                                     @RequestBody PointsUpdateRequest request) {
        try {
            customerService.updateCustomerPoints(id, request.getPoints());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<CustomerDTO>> getActiveCustomers() {
        List<CustomerDTO> customers = customerService.getActiveCustomers();
        return ResponseEntity.ok(customers);
    }
    

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Optional<CustomerDTO> existingCustomer = customerService.getCustomerByEmail(request.getEmail());
            if (existingCustomer.isPresent() && existingCustomer.get().getStatus() == 1) {
                return ResponseEntity.badRequest().body("Email đã được sử dụng");
            }
            
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setName(request.getName());
            customerDTO.setEmail(request.getEmail());
            customerDTO.setPhone(request.getPhone());
            customerDTO.setAddress(request.getAddress());
            customerDTO.setPassword(request.getPassword());
            customerDTO.setPoints(0);
            customerDTO.setStatus(0);
            
            CustomerDTO createdCustomer = customerService.createCustomer(customerDTO);

            authService.sendOTP(request.getEmail());
            
            return ResponseEntity.ok("Đăng ký thành công. Vui lòng kiểm tra email để xác thực OTP.");
            
        } catch (Exception e) {
            System.out.println("Register error details: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi đăng ký: " + e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPRequest request) {
        try {
            if (authService.verifyOTP(request.getEmail(), request.getOtp())) {
                customerService.activateCustomerByEmail(request.getEmail());
                return ResponseEntity.ok("Đã xác thực thành công!");
            }
            return ResponseEntity.badRequest().body("OTP không đúng");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("OTP không đúng");
        }
    }

    @PostMapping("/{id}/resend-otp")
    public ResponseEntity<?> resendOTP(@PathVariable String id) {
        try {
            Optional<CustomerDTO> customer = customerService.getCustomerById(id);
            if (customer.isEmpty()) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            if (customer.get().getEmail() == null || customer.get().getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            authService.sendOTP(customer.get().getEmail());
            return ResponseEntity.ok("Đã gửi mã OTP qua email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("OTP không đúng");
        }
    }

    @PostMapping("/{id}/verify-activation")
    public ResponseEntity<?> verifyActivation(@PathVariable String id, @RequestBody VerifyOTPRequest request) {
        try {
            Optional<CustomerDTO> customer = customerService.getCustomerById(id);
            if (customer.isEmpty()) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            if (authService.verifyOTP(customer.get().getEmail(), request.getOtp())) {
                customerService.activateCustomerByEmail(customer.get().getEmail());
                return ResponseEntity.ok("Đã xác thực thành công!");
            }
            return ResponseEntity.badRequest().body("OTP không đúng");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("OTP không đúng");
        }
    }

    public static class PointsUpdateRequest {
        private Integer points;
        
        public Integer getPoints() {
            return points;
        }
        
        public void setPoints(Integer points) {
            this.points = points;
        }
    }
    
    public static class RegisterRequest {
        private String name;
        private String email;
        private String phone;
        private String address;
        private String password;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class VerifyOTPRequest {
        private String email;
        private String otp;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
}
