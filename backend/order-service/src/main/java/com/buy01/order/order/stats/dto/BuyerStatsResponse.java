package com.buy01.order.order.stats.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for {@code GET /orders/stats/me}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerStatsResponse {

    private BigDecimal totalSpent;
    private long ordersCount;
    private List<ProductStatsResponse> mostBoughtProducts;
    private List<ProductStatsResponse> bestProducts;
    private List<CategoryStatsResponse> topCategories;
}
