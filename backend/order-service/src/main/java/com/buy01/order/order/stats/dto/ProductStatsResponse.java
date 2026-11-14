package com.buy01.order.order.stats.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product row used by buyer statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatsResponse {

    private String productId;
    private String name;
    private String imageUrl;
    private long quantity;
    private BigDecimal amountSpent;
}
