package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Cart;
import com.example.Bookstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByCustomerAndStatus(Customer customer, Integer status);
}

