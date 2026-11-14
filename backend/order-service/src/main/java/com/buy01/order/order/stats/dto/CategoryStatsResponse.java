package com.buy01.order.order.stats.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category row used by buyer statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsResponse {

    private String category;
    private long quantity;
    private BigDecimal amountSpent;
}
