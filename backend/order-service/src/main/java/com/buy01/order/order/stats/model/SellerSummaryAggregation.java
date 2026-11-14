package com.buy01.order.order.stats.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerSummaryAggregation {

    private BigDecimal totalRevenue;
    private long ordersCount;
    private long unitsSold;
}
