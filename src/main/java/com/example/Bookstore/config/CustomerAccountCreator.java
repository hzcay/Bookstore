package com.example.Bookstore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerAccountCreator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if test customer exists
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE email = ?", 
                Integer.class, 
                "test@customer.com"
            );
            
            if (count == 0) {
                // Create test customer using raw SQL
                String hashedPassword = passwordEncoder.encode("123456");
                jdbcTemplate.update(
                    "INSERT INTO customers (customerid, name, email, phone, password, address, points, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "CUST001",
                    "Test Customer", 
                    "test@customer.com",
                    "0123456789",
                    hashedPassword,
                    "123 Test Street",
                    0,
                    1
                );
                System.out.println("✅ Test customer account created successfully!");
                System.out.println("📧 Email: test@customer.com");
                System.out.println("🔑 Password: 123456");
            } else {
                System.out.println("ℹ️ Test customer account already exists, skipping creation.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating test customer account: " + e.getMessage());
        }
    }
}
