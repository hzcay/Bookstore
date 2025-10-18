package com.example.Bookstore.config;

import com.example.Bookstore.entity.Employee;
import com.example.Bookstore.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// @Component  // Disabled to avoid conflict
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            if (!employeeRepository.findByEmail("bacdoan52@gmail.com").isPresent()) {
                Employee admin = new Employee();
                admin.setEmployeeId("EMP001");
                admin.setName("Nguyễn Văn Hiếu");
                admin.setEmail("bacdoan52@gmail.com");
                admin.setPhone("0869074687");
                admin.setPassword(passwordEncoder.encode("Hzcay@123"));
                admin.setRole("ADMIN");
                admin.setStatus(1);
                
                employeeRepository.saveAndFlush(admin);
                System.out.println("✅ Admin account created successfully!");
                System.out.println("📧 Email: bacdoan52@gmail.com");
                System.out.println("🔑 Password: Hzcay@123");
            } else {
                System.out.println("ℹ️ Admin account already exists, skipping creation.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating admin account: " + e.getMessage());
            System.out.println("ℹ️ You may need to create admin account manually.");
            // Don't rethrow the exception to prevent app crash
        }
    }
}
