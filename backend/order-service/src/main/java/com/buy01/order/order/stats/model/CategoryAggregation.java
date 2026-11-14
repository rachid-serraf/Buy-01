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
public class CategoryAggregation {

    private String category;
    private long quantity;
    private BigDecimal amountSpent;
}
