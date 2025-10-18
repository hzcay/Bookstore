package com.example.Bookstore.service.impl;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpSession;

import com.example.Bookstore.dto.CartDTO;
import com.example.Bookstore.dto.CartItemDTO;
import com.example.Bookstore.entity.Cart;
import com.example.Bookstore.entity.CartItem;
import com.example.Bookstore.entity.Customer;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.CartRepository;
import com.example.Bookstore.repository.CustomerRepository;
import com.example.Bookstore.service.CartService;

import static com.example.Bookstore.controller.AuthController.SESSION_UID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private static final String KEY = "CART";

    private final BookRepository bookRepo;
    private final CartRepository cartRepo;
    private final CustomerRepository customerRepo;

    private String uid(HttpSession s) {
        Object v = s.getAttribute(SESSION_UID);
        return v == null ? null : String.valueOf(v);
    }

    private Customer currentCustomer(HttpSession s) {
        String id = uid(s);
        return (id == null) ? null : customerRepo.findById(id).orElse(null);
    }

    private CartDTO ensureSessionCart(HttpSession s) {
        CartDTO c = (CartDTO) s.getAttribute(KEY);
        if (c == null) {
            c = new CartDTO(new LinkedHashMap<>());
            s.setAttribute(KEY, c);

            var me = currentCustomer(s);
            if (me != null) {
                var opt = cartRepo.findByCustomerAndStatus(me, 1);
                if (opt.isPresent()) {
                    var db = opt.get();
                    var map = new LinkedHashMap<String, CartItemDTO>();
                    if (db.getItems() != null) {
                        for (var ci : db.getItems()) {
                            var b = ci.getBook();
                            map.put(
                                    b.getBookId(),
                                    CartItemDTO.builder()
                                            .cartItemId(ci.getCartItemId())
                                            .bookId(b.getBookId())
                                            .title(b.getTitle())
                                            .price(ci.getPrice() == null ? 0d : ci.getPrice())
                                            .qty(ci.getQuantity() == null ? 0 : ci.getQuantity())
                                            .thumbnail(b.getThumbnail())
                                            .stock(b.getQuantity())
                                            .build());
                        }
                    }
                    c.setItems(map);
                }
            }
        }
        return c;
    }

    private double nz(Double d) {
        return d == null ? 0d : d;
    }

    private int nzi(Integer i) {
        return i == null ? 0 : i;
    }

    @Override
    public CartDTO getCart(HttpSession s) {
        return ensureSessionCart(s);
    }

    @Override
    public CartDTO add(HttpSession s, String bookId, int qty) {
        var cart = ensureSessionCart(s);
        var b = bookRepo.findById(bookId).orElseThrow();

        double price = nz(b.getSalePrice());
        int stock = b.getQuantity() == null ? Integer.MAX_VALUE : b.getQuantity();

        var item = cart.getItems().getOrDefault(
                bookId,
                CartItemDTO.builder()
                        .bookId(bookId)
                        .title(b.getTitle())
                        .price(price)
                        .qty(0)
                        .stock(stock)
                        .thumbnail(b.getThumbnail())
                        .build());

        int safeQty = Math.max(qty, 1);
        int curQty = nzi(item.getQty());
        int maxStock = item.getStock() == null ? Integer.MAX_VALUE : item.getStock();
        item.setQty(Math.min(curQty + safeQty, maxStock));

        cart.getItems().put(bookId, item);

        mirrorToDbIfLogged(s, cart);
        return cart;
    }

    @Override
    public CartDTO update(HttpSession s, String bookId, int qty) {
        var cart = ensureSessionCart(s);
        var it = cart.getItems().get(bookId);
        if (it != null) {
            if (qty <= 0) {
                cart.getItems().remove(bookId);
            } else {
                int maxStock = it.getStock() == null ? Integer.MAX_VALUE : it.getStock();
                it.setQty(Math.min(qty, maxStock));
            }
        }
        mirrorToDbIfLogged(s, cart);
        return cart;
    }

    @Override
    public CartDTO remove(HttpSession s, String bookId) {
        var c = ensureSessionCart(s);
        c.getItems().remove(bookId);
        mirrorToDbIfLogged(s, c);
        return c;
    }

    @Override
    public void clear(HttpSession s) {
        s.removeAttribute(KEY);
        var me = currentCustomer(s);
        if (me != null) {
            cartRepo.findByCustomerAndStatus(me, 1).ifPresent(cartRepo::delete);
        }
    }

    private void mirrorToDbIfLogged(HttpSession s, CartDTO sessionCart) {
        var me = currentCustomer(s);
        if (me == null)
            return;

        Cart dbCart = cartRepo.findByCustomerAndStatus(me, 1).orElseGet(() -> {
            Cart c = new Cart();
            c.setCustomer(me);
            c.setStatus(1);
            return c;
        });

        dbCart.getItems().clear();
        double total = 0d;

        for (CartItemDTO dto : sessionCart.getItems().values()) {
            var book = bookRepo.findById(dto.getBookId()).orElse(null);
            if (book == null)
                continue;

            CartItem ci = new CartItem();
            ci.setCart(dbCart);
            ci.setBook(book);
            ci.setQuantity(nzi(dto.getQty()));
            ci.setPrice(nz(dto.getPrice()));
            total += nz(ci.getPrice()) * nzi(ci.getQuantity());

            dbCart.getItems().add(ci);
        }
        dbCart.setTotal(total);

        cartRepo.save(dbCart);
    }
}

