package com.buy01.order.order.stats.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One point in the seller revenue chart.
 * The month is an ISO value such as {@code 2026-11}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueResponse {

    private String month;
    private BigDecimal revenue;
    private long unitsSold;
}
