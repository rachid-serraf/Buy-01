package com.buy01.order.order.stats.repository;

import java.time.Instant;
import java.util.List;

import com.buy01.order.order.stats.model.BuyerSummaryAggregation;
import com.buy01.order.order.stats.model.CategoryAggregation;
import com.buy01.order.order.stats.model.MonthlyAggregation;
import com.buy01.order.order.stats.model.ProductAggregation;
import com.buy01.order.order.stats.model.SellerSummaryAggregation;

public interface OrderStatsRepository {

    BuyerSummaryAggregation buyerSummary(String buyerId);

    List<ProductAggregation> buyerMostBoughtProducts(String buyerId);

    List<ProductAggregation> buyerBestProducts(String buyerId);

    List<CategoryAggregation> buyerTopCategories(String buyerId);

    SellerSummaryAggregation sellerSummary(String sellerId);

    List<ProductAggregation> sellerBestSellingProducts(String sellerId);

    List<MonthlyAggregation> sellerRevenueByMonth(String sellerId, Instant fromInclusive);
}
