package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Cart;
import com.example.Bookstore.entity.CartItem;
import com.example.Bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    Optional<CartItem> findByCartAndBook(Cart cart, Book book);
}

