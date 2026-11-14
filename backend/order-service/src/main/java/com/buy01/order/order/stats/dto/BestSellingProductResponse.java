package com.buy01.order.order.stats.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product row used by seller statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestSellingProductResponse {

    private String productId;
    private String name;
    private String imageUrl;
    private long unitsSold;
    private BigDecimal revenue;
}
