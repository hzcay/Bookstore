package com.example.Bookstore.dto;

import com.example.Bookstore.entity.Cart;
import com.example.Bookstore.entity.CartItem;
import lombok.*;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cartId;

    @Builder.Default
    private Map<String, CartItemDTO> items = new LinkedHashMap<>();

    public CartDTO(Map<String, CartItemDTO> items) {
        this.items = (items != null) ? items : new LinkedHashMap<>();
    }

    public int getTotalQty() {
        return items.values().stream()
                .mapToInt(i -> Objects.requireNonNullElse(i.getQty(), 0))
                .sum();
    }

    public double getSubtotal() {
        return items.values().stream()
                .mapToDouble(i -> Objects.requireNonNullElse(i.getPrice(), 0d)
                        * Objects.requireNonNullElse(i.getQty(), 0))
                .sum();
    }

    public double getTotal() {
        return getSubtotal();
    }

    public static CartDTO fromEntity(Cart cart) {
        CartDTO dto = new CartDTO();
        if (cart == null) return dto;

        dto.setCartId(cart.getCartId());
        Map<String, CartItemDTO> map = new LinkedHashMap<>();
        if (cart.getItems() != null) {
            for (CartItem it : cart.getItems()) {
                map.put(it.getCartItemId(), CartItemDTO.fromEntity(it));
            }
        }
        dto.setItems(map);
        return dto;
    }

    public static CartDTO from(Cart cart) {
        return fromEntity(cart);
    }
}

