package com.buy01.order.cart.model.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
public class CartItem {

    private String productId;
    private String sellerId;          // product.userId
    private String name;
    private BigDecimal unitPrice;
    private String imageId;
    private int quantity;
    private String category;

    /**
     * Backwards-compatible constructor for callers that do not have a category.
     */
    public CartItem(
            String productId,
            String sellerId,
            String name,
            BigDecimal unitPrice,
            String imageId,
            int quantity) {
        this(productId, sellerId, name, unitPrice, imageId, quantity, null);
    }
}
