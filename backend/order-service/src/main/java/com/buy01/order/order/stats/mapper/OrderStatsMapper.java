package com.buy01.order.order.stats.mapper;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.buy01.order.order.stats.dto.BestSellingProductResponse;
import com.buy01.order.order.stats.dto.BuyerStatsResponse;
import com.buy01.order.order.stats.dto.CategoryStatsResponse;
import com.buy01.order.order.stats.dto.MonthlyRevenueResponse;
import com.buy01.order.order.stats.dto.ProductStatsResponse;
import com.buy01.order.order.stats.dto.SellerStatsResponse;
import com.buy01.order.order.stats.model.BuyerSummaryAggregation;
import com.buy01.order.order.stats.model.CategoryAggregation;
import com.buy01.order.order.stats.model.MonthlyAggregation;
import com.buy01.order.order.stats.model.ProductAggregation;
import com.buy01.order.order.stats.model.SellerSummaryAggregation;

@Component
public class OrderStatsMapper {

    public BuyerStatsResponse toBuyerStats(
            BuyerSummaryAggregation summary,
            List<ProductAggregation> mostBought,
            List<ProductAggregation> best,
            List<CategoryAggregation> categories) {

        return BuyerStatsResponse.builder()
                .totalSpent(money(summary == null ? null : summary.getTotalSpent()))
                .ordersCount(summary == null ? 0L : summary.getOrdersCount())
                .mostBoughtProducts(toProductStats(mostBought))
                .bestProducts(toProductStats(best))
                .topCategories(toCategoryStats(categories))
                .build();
    }

    public SellerStatsResponse toSellerStats(
            SellerSummaryAggregation summary,
            List<ProductAggregation> bestSelling,
            List<MonthlyAggregation> monthly,
            YearMonth firstMonth,
            int months) {

        return SellerStatsResponse.builder()
                .totalRevenue(money(summary == null ? null : summary.getTotalRevenue()))
                .ordersCount(summary == null ? 0L : summary.getOrdersCount())
                .unitsSold(summary == null ? 0L : summary.getUnitsSold())
                .bestSellingProducts(toBestSellingProducts(bestSelling))
                .revenueByMonth(toMonthlyRevenue(monthly, firstMonth, months))
                .build();
    }

    private List<ProductStatsResponse> toProductStats(List<ProductAggregation> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .map(row -> ProductStatsResponse.builder()
                        .productId(row.getProductId())
                        .name(row.getName())
                        .imageUrl(row.getImageUrl())
                        .quantity(row.getQuantity())
                        .amountSpent(money(row.getAmountSpent()))
                        .build())
                .toList();
    }

    private List<BestSellingProductResponse> toBestSellingProducts(
            List<ProductAggregation> rows) {

        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .map(row -> BestSellingProductResponse.builder()
                        .productId(row.getProductId())
                        .name(row.getName())
                        .imageUrl(row.getImageUrl())
                        .unitsSold(row.getQuantity())
                        .revenue(money(row.getAmountSpent()))
                        .build())
                .toList();
    }

    private List<CategoryStatsResponse> toCategoryStats(List<CategoryAggregation> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .map(row -> CategoryStatsResponse.builder()
                        .category(row.getCategory())
                        .quantity(row.getQuantity())
                        .amountSpent(money(row.getAmountSpent()))
                        .build())
                .toList();
    }

    private List<MonthlyRevenueResponse> toMonthlyRevenue(
            List<MonthlyAggregation> rows,
            YearMonth firstMonth,
            int months) {

        Map<String, MonthlyAggregation> byMonth = new HashMap<>();
        if (rows != null) {
            for (MonthlyAggregation row : rows) {
                if (row != null && row.getMonth() != null) {
                    byMonth.put(row.getMonth(), row);
                }
            }
        }

        List<MonthlyRevenueResponse> result = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            String month = firstMonth.plusMonths(i).toString();
            MonthlyAggregation row = byMonth.get(month);

            result.add(MonthlyRevenueResponse.builder()
                    .month(month)
                    .revenue(row == null ? BigDecimal.ZERO : money(row.getRevenue()))
                    .unitsSold(row == null ? 0L : row.getUnitsSold())
                    .build());
        }

        return result;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
