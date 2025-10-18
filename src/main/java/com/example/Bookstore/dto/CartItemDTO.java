package com.example.Bookstore.dto;

import com.example.Bookstore.entity.CartItem;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDTO {
    private String cartItemId;
    private String bookId;
    private String title;
    private Integer qty;
    private Double price;
    private String thumbnail;
    private Integer stock;

    public double getSubtotal() {
        double p = price == null ? 0d : price;
        int q = qty == null ? 0 : qty;
        return p * q;
    }

    public static CartItemDTO fromEntity(CartItem i) {
        if (i == null) return CartItemDTO.builder().build();
        return CartItemDTO.builder()
                .cartItemId(i.getCartItemId())
                .bookId(i.getBook().getBookId())
                .title(i.getBook().getTitle())
                .qty(i.getQuantity())
                .price(i.getPrice())
                .thumbnail(i.getBook().getThumbnail())
                .stock(null)
                .build();
    }

    public static CartItemDTO from(CartItem i) {
        return fromEntity(i);
    }
}

