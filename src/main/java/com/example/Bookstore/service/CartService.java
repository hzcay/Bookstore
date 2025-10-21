package com.example.Bookstore.service;

import com.example.Bookstore.dto.CartDTO;

import jakarta.servlet.http.HttpSession;
import java.util.Set;

public interface CartService {
    CartDTO getCart(HttpSession session);

    CartDTO add(HttpSession session, String bookId, int qty);

    CartDTO update(HttpSession session, String bookId, int qty);

    CartDTO remove(HttpSession session, String bookId);

    void removeItems(HttpSession session, Set<String> bookIds);

    void clear(HttpSession session);

}

