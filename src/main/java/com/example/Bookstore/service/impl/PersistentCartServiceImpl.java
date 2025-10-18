package com.example.Bookstore.service.impl;

import com.example.Bookstore.entity.*;
import com.example.Bookstore.repository.*;
import com.example.Bookstore.service.PersistentCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PersistentCartServiceImpl implements PersistentCartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final BookRepository bookRepo;

    @Override
    public Cart getOrCreate(Customer customer) {
        return cartRepo.findByCustomerAndStatus(customer, 1)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setCustomer(customer);
                    c.setStatus(1);
                    return cartRepo.save(c);
                });
    }

    @Override
    public Cart addItem(Customer customer, String bookId, int qty) {
        if (qty <= 0)
            qty = 1;
        Cart cart = getOrCreate(customer);
        Book book = bookRepo.findById(bookId).orElseThrow();

        var opt = cartItemRepo.findByCartAndBook(cart, book);
        if (opt.isPresent()) {
            CartItem it = opt.get();
            it.setQuantity(it.getQuantity() + qty);
        } else {
            CartItem it = new CartItem();
            it.setCart(cart);
            it.setBook(book);
            it.setQuantity(qty);
            it.setPrice(book.getSalePrice() != null ? book.getSalePrice() : 0.0);
            cart.getItems().add(it);
        }
        return cartRepo.save(cart);
    }

    @Override
    public Cart setQty(Customer customer, String cartItemId, int qty) {
        Cart cart = getOrCreate(customer);
        CartItem item = cartItemRepo.findById(cartItemId).orElseThrow();
        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new IllegalStateException("Item không thuộc cart của user này");
        }
        if (qty <= 0) {
            cart.getItems().remove(item);
            cartItemRepo.delete(item);
        } else {
            item.setQuantity(qty);
        }
        return cartRepo.save(cart);
    }

    @Override
    public void clear(Customer me) {
        Cart cart = getOrCreate(me);
        cart.getItems().clear();
        cartRepo.save(cart);
    }

}

