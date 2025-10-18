package com.example.Bookstore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccountCreator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if admin exists
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE email = ?", 
                Integer.class, 
                "bacdoan52@gmail.com"
            );
            
            if (count == 0) {
                // Create admin using raw SQL
                String hashedPassword = passwordEncoder.encode("Hzcay@123");
                jdbcTemplate.update(
                    "INSERT INTO employees (employeeid, name, email, phone, password, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "EMP001",
                    "Nguyễn Văn Hiếu", 
                    "bacdoan52@gmail.com",
                    "0869074687",
                    hashedPassword,
                    "ADMIN",
                    1
                );
                System.out.println("✅ Admin account created successfully!");
                System.out.println("📧 Email: bacdoan52@gmail.com");
                System.out.println("🔑 Password: Hzcay@123");
            } else {
                System.out.println("ℹ️ Admin account already exists, skipping creation.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating admin account: " + e.getMessage());
            System.out.println("ℹ️ You may need to create admin account manually.");
        }
    }
}
