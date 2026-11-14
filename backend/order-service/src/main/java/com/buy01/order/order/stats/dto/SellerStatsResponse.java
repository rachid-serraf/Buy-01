package com.buy01.order.order.stats.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for {@code GET /orders/stats/seller}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatsResponse {

    private BigDecimal totalRevenue;
    private long ordersCount;
    private long unitsSold;
    private List<BestSellingProductResponse> bestSellingProducts;
    private List<MonthlyRevenueResponse> revenueByMonth;
}
