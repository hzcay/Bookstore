package com.example.Bookstore.controller;

import com.example.Bookstore.dto.CartDTO;
import com.example.Bookstore.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.Bookstore.service.OrderService;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Arrays;
import jakarta.validation.Valid;
import com.example.Bookstore.dto.CheckoutRequest;
import com.example.Bookstore.dto.OrderPlacedDTO;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

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

    @PostMapping("/update")
    public ResponseEntity<?> updateQtyFlexible(
            HttpSession session,
            @RequestParam(value = "bookId", required = false) String bookIdQP,
            @RequestParam(value = "qty",     required = false) Integer qtyQP,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String bookId = bookIdQP;
        Integer qty   = qtyQP;

        if (body != null) {
            if (bookId == null && body.get("bookId") != null) {
                bookId = String.valueOf(body.get("bookId"));
            }
            if (qty == null) {
                Object q = body.get("qty");
                if (q == null) q = body.get("quantity");
                if (q != null) qty = Integer.valueOf(String.valueOf(q));
            }
        }

        if (bookId == null || qty == null) {
            return ResponseEntity.badRequest().body("Missing bookId/qty");
        }

        cartService.update(session, bookId, Math.max(1, qty));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<?> removeItems(
            HttpSession session,
            @RequestParam(value = "ids", required = false) String idsCsv,
            @RequestBody(required = false) List<String> idsBody
    ) {
        Set<String> ids = new LinkedHashSet<>();
        if (idsBody != null) {
            idsBody.stream().filter(Objects::nonNull).map(String::valueOf).forEach(ids::add);
        }
        if (idsCsv != null && !idsCsv.isBlank()) {
            Arrays.stream(idsCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return ResponseEntity.badRequest().body("No ids provided");
        }
        cartService.removeItems(session, ids);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/checkout")
    public ResponseEntity<OrderPlacedDTO> checkout(
            HttpSession session,
            @Valid @RequestBody CheckoutRequest req,
            @RequestParam(value = "ids", required = false) String idsCsv
    ) {
        if ((req.getSelectedIds() == null || req.getSelectedIds().isEmpty())
                && idsCsv != null && !idsCsv.isBlank()) {
            List<String> ids = Arrays.stream(idsCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            req.setSelectedIds(ids);
        }
        OrderPlacedDTO placed = orderService.guestCheckout(req, session);
        return ResponseEntity.ok(placed);
    }
}

