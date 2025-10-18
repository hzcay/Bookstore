package com.example.Bookstore.controller;

import com.example.Bookstore.dto.CustomerDTO;
import com.example.Bookstore.dto.EmployeeDTO;
import com.example.Bookstore.entity.Customer;
import com.example.Bookstore.repository.CustomerRepository;
import com.example.Bookstore.service.CustomerService;
import com.example.Bookstore.service.EmployeeService;
import com.example.Bookstore.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    public static final String SESSION_UID = "AUTH_CUSTOMER_ID";
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CustomerRepository customerRepo;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Optional<CustomerDTO> existingCustomer = customerService.getCustomerByEmail(request.getEmail());
            
            if (existingCustomer.isPresent()) {
                CustomerDTO existing = existingCustomer.get();
                if (existing.getStatus() == 1) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng"));
                }
                existing.setName(request.getName());
                existing.setPhone(request.getPhone());
                existing.setAddress(request.getAddress());
                existing.setPassword(request.getPassword());
                existing.setStatus(0);
                customerService.updateCustomer(existing.getCustomerId(), existing);
                authService.sendOTP(request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin và gửi mã OTP thành công"));
            }
            
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setName(request.getName());
            customerDTO.setEmail(request.getEmail());
            customerDTO.setPhone(request.getPhone());
            customerDTO.setAddress(request.getAddress());
            customerDTO.setPassword(request.getPassword());
            customerDTO.setPoints(0);
            customerDTO.setStatus(0);
            
            customerService.createCustomer(customerDTO);
            authService.sendOTP(request.getEmail());
            
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công. Vui lòng kiểm tra email để xác thực OTP."));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi đăng ký: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPRequest request) {
        try {
            if (authService.verifyOTP(request.getEmail(), request.getOtp())) {
                customerService.activateCustomerByEmail(request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Đã xác thực thành công!"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "OTP không đúng"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi xác thực OTP"));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOTP(@RequestBody ResendOTPRequest request) {
        try {
            authService.sendOTP(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Đã gửi lại mã OTP"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi gửi OTP"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        try {
            Optional<CustomerDTO> customer = customerService.getCustomerByEmailForAuth(request.getEmail());
            
            if (customer.isPresent()) {
                CustomerDTO cust = customer.get();
                if (cust.getStatus() == 0) {
                    if (passwordEncoder.matches(request.getPassword(), cust.getPassword())) {
                        authService.sendOTP(request.getEmail());
                        Map<String, Object> response = new HashMap<>();
                        response.put("needVerification", true);
                        response.put("email", request.getEmail());
                        response.put("message", "Tài khoản chưa xác thực. Mã OTP đã được gửi qua email.");
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("message", "Email hoặc mật khẩu không đúng"));
                    }
                } else {
                    if (passwordEncoder.matches(request.getPassword(), cust.getPassword())) {
                        session.setAttribute(SESSION_UID, cust.getCustomerId());
                        session.setAttribute("userId", cust.getCustomerId());
                        session.setAttribute("userType", "CUSTOMER");
                        session.setAttribute("userName", cust.getName());
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("userType", "CUSTOMER");
                        response.put("userId", cust.getCustomerId());
                        response.put("userName", cust.getName());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("message", "Email hoặc mật khẩu không đúng"));
                    }
                }
            }
            
            Optional<EmployeeDTO> employee = employeeService.getEmployeeByEmailForAuth(request.getEmail());
            if (employee.isPresent()) {
                EmployeeDTO emp = employee.get();
                if (emp.getStatus() == 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản nhân viên đã bị vô hiệu hóa"));
                } else {
                    if (passwordEncoder.matches(request.getPassword(), emp.getPassword())) {
                        session.setAttribute("userId", emp.getEmployeeId());
                        session.setAttribute("userType", "EMPLOYEE");
                        session.setAttribute("userName", emp.getName());
                        session.setAttribute("userRole", emp.getRole());
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("userType", "EMPLOYEE");
                        response.put("userId", emp.getEmployeeId());
                        response.put("userName", emp.getName());
                        response.put("role", emp.getRole());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("message", "Email hoặc mật khẩu không đúng"));
                    }
                }
            }
            
            return ResponseEntity.badRequest().body(Map.of("message", "Email hoặc mật khẩu không đúng"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi đăng nhập: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-login")
    public ResponseEntity<?> verifyLogin(@RequestBody VerifyLoginRequest request, HttpSession session) {
        try {
            if (!authService.verifyOTP(request.getEmail(), request.getOtp())) {
                return ResponseEntity.badRequest().body("OTP không đúng");
            }
            
            Optional<CustomerDTO> customer = customerService.getCustomerByEmailForAuth(request.getEmail());
            if (customer.isPresent()) {
                CustomerDTO cust = customer.get();
                customerService.activateCustomerByEmail(request.getEmail());
                
                session.setAttribute(SESSION_UID, cust.getCustomerId());
                session.setAttribute("userId", cust.getCustomerId());
                session.setAttribute("userType", "CUSTOMER");
                session.setAttribute("userName", cust.getName());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userType", "CUSTOMER");
                response.put("userId", cust.getCustomerId());
                response.put("userName", cust.getName());
                return ResponseEntity.ok(response);
            }
            
            // Employee không cần verify OTP - chỉ customer mới cần
            
            return ResponseEntity.badRequest().body("Không tìm thấy tài khoản");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xác thực");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            Optional<CustomerDTO> customer = customerService.getCustomerByEmailForAuth(request.getEmail());
            if (customer.isPresent()) {
                if (customer.get().getStatus() == 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email chưa được xác thực. Vui lòng xác thực tài khoản trước."));
                }
                authService.sendOTP(request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi qua email"));
            }
            
            Optional<EmployeeDTO> employee = employeeService.getEmployeeByEmailForAuth(request.getEmail());
            if (employee.isPresent()) {
                if (employee.get().getStatus() == 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email chưa được xác thực. Vui lòng xác thực tài khoản trước."));
                }
                authService.sendOTP(request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi qua email"));
            }
            
            return ResponseEntity.badRequest().body(Map.of("message", "Email không tồn tại trong hệ thống"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi gửi OTP"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            if (!authService.verifyOTP(request.getEmail(), request.getOtp())) {
                return ResponseEntity.badRequest().body(Map.of("message", "OTP không đúng"));
            }
            
            Optional<CustomerDTO> customer = customerService.getCustomerByEmailForAuth(request.getEmail());
            if (customer.isPresent()) {
                CustomerDTO cust = customer.get();
                cust.setPassword(request.getNewPassword());
                customerService.updateCustomer(cust.getCustomerId(), cust);
                return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
            }
            
            Optional<EmployeeDTO> employee = employeeService.getEmployeeByEmailForAuth(request.getEmail());
            if (employee.isPresent()) {
                EmployeeDTO emp = employee.get();
                emp.setPassword(request.getNewPassword());
                employeeService.updateEmployee(emp.getEmployeeId(), emp);
                return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
            }
            
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy tài khoản"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi đổi mật khẩu"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Đăng xuất thành công");
    }

    @GetMapping("/session")
    public ResponseEntity<?> getSession(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");
        String userName = (String) session.getAttribute("userName");
        String userRole = (String) session.getAttribute("userRole");
        
        if (userId == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("userId", userId);
        response.put("userType", userType);
        response.put("userName", userName);
        if (userRole != null) {
            response.put("role", userRole);
        }
        return ResponseEntity.ok(response);
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

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        String uid = (String) session.getAttribute(SESSION_UID);
        if (uid == null)
            throw new RuntimeException("Unauthenticated");

        Customer c = customerRepo.findByCustomerIdAndStatus(uid, 1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Map.of(
                "customerId", c.getCustomerId(),
                "name", c.getName(),
                "email", c.getEmail(),
                "phone", c.getPhone() != null ? c.getPhone() : "",
                "points", c.getPoints() != null ? c.getPoints() : 0,
                "address", c.getAddress() != null ? c.getAddress() : "");
    }
    
    public static class VerifyOTPRequest {
        private String email;
        private String otp;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ResendOTPRequest {
        private String email;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class LoginRequest {
        private String email;
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class VerifyLoginRequest {
        private String email;
        private String otp;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ForgotPasswordRequest {
        private String email;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}

