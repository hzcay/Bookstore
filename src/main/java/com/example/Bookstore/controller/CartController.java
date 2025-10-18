package com.example.Bookstore.controller;

import com.example.Bookstore.dto.CartDTO;
import com.example.Bookstore.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartDTO myCart(HttpSession session) {
        return cartService.getCart(session);
    }

    @PostMapping("/add")
    public CartDTO addPost(@RequestParam String bookId,
                           @RequestParam(defaultValue = "1") int qty,
                           HttpSession session) {
        return cartService.add(session, bookId, qty);
    }

    @GetMapping("/add")
    public CartDTO addGet(@RequestParam String bookId,
                          @RequestParam(defaultValue = "1") int qty,
                          HttpSession session) {
        return cartService.add(session, bookId, qty);
    }

    @PatchMapping("/item/{id}")
    public CartDTO setQty(@PathVariable("id") String bookId,
                          @RequestParam int qty,
                          HttpSession session) {
        return cartService.update(session, bookId, qty);
    }

    @DeleteMapping("/clear")
    public void clear(HttpSession session) {
        cartService.clear(session);
    }
}

