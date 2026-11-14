package com.buy01.order.order.model.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private String productId;
    private String sellerId;
    private String name;
    private String imageUrl;
    private String category;
    private BigDecimal price;
    private int quantity;
}
