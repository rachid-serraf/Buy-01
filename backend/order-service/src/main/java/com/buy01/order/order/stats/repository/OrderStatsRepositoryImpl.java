package com.buy01.order.order.stats.repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import com.buy01.order.order.model.entity.Order;
import com.buy01.order.order.stats.OrderStatsConstants;
import com.buy01.order.order.stats.model.BuyerSummaryAggregation;
import com.buy01.order.order.stats.model.CategoryAggregation;
import com.buy01.order.order.stats.model.MonthlyAggregation;
import com.buy01.order.order.stats.model.ProductAggregation;
import com.buy01.order.order.stats.model.SellerSummaryAggregation;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderStatsRepositoryImpl implements OrderStatsRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public BuyerSummaryAggregation buyerSummary(String buyerId) {
        Aggregation aggregation = newAggregation(
                match(revenueCriteria("buyerId", buyerId)),
                group()
                        .sum(toDecimal("total")).as("totalSpent")
                        .count().as("ordersCount"));

        return mongoTemplate
                .aggregate(aggregation, Order.class, BuyerSummaryAggregation.class)
                .getUniqueMappedResult();
    }

    @Override
    public List<ProductAggregation> buyerMostBoughtProducts(String buyerId) {
        return productAggregation(
                revenueCriteria("buyerId", buyerId),
                null,
                Sort.Direction.DESC,
                "quantity");
    }

    @Override
    public List<ProductAggregation> buyerBestProducts(String buyerId) {
        return productAggregation(
                revenueCriteria("buyerId", buyerId),
                null,
                Sort.Direction.DESC,
                "amountSpent");
    }

    @Override
    public List<CategoryAggregation> buyerTopCategories(String buyerId) {
        ProjectionOperation prepare = project()
                .and("items.category").as("category")
                .and("items.quantity").as("quantity")
                .and(itemAmount()).as("amountSpent");

        GroupOperation group = group("category")
                .sum(toDecimal("quantity")).as("quantity")
                .sum("amountSpent").as("amountSpent");

        ProjectionOperation project = project()
                .and("_id").as("category")
                .and("quantity").as("quantity")
                .and("amountSpent").as("amountSpent")
                .andExclude("_id");

        Aggregation aggregation = newAggregation(
                match(revenueCriteria("buyerId", buyerId)),
                unwind("items"),
                prepare,
                group,
                project,
                sort(Sort.by(Sort.Direction.DESC, "amountSpent")),
                limit(OrderStatsConstants.TOP_LIMIT));

        return mongoTemplate
                .aggregate(aggregation, Order.class, CategoryAggregation.class)
                .getMappedResults();
    }

    @Override
    public SellerSummaryAggregation sellerSummary(String sellerId) {
        GroupOperation perOrder = group("_id")
                .sum(itemAmount()).as("orderRevenue")
                .sum(toDecimal("items.quantity")).as("orderUnits");

        GroupOperation totals = group()
                .sum("orderRevenue").as("totalRevenue")
                .sum("orderUnits").as("unitsSold")
                .count().as("ordersCount");

        Aggregation aggregation = newAggregation(
                match(revenueCriteria("sellerId", sellerId)),
                unwind("items"),
                match(Criteria.where("items.sellerId").is(sellerId)),
                perOrder,
                totals);

        return mongoTemplate
                .aggregate(aggregation, Order.class, SellerSummaryAggregation.class)
                .getUniqueMappedResult();
    }

    @Override
    public List<ProductAggregation> sellerBestSellingProducts(String sellerId) {
        return productAggregation(
                revenueCriteria("sellerId", sellerId),
                sellerId,
                Sort.Direction.DESC,
                "quantity");
    }

    @Override
    public List<MonthlyAggregation> sellerRevenueByMonth(String sellerId, Instant fromInclusive) {
        Criteria criteria = revenueCriteria("sellerId", sellerId)
                .and("createdAt").gte(fromInclusive);

        ProjectionOperation prepare = project()
                .and(DateOperators.DateToString.dateOf("createdAt")
                        .toString("%Y-%m")
                        .withTimezone(DateOperators.Timezone.valueOf("UTC")))
                .as("month")
                .and("items.quantity").as("quantity")
                .and(itemAmount()).as("amountSpent");

        GroupOperation group = group("month")
                .sum("amountSpent").as("revenue")
                .sum(toDecimal("quantity")).as("unitsSold");

        ProjectionOperation project = project()
                .and("_id").as("month")
                .and("revenue").as("revenue")
                .and("unitsSold").as("unitsSold")
                .andExclude("_id");

        Aggregation aggregation = newAggregation(
                match(criteria),
                unwind("items"),
                match(Criteria.where("items.sellerId").is(sellerId)),
                prepare,
                group,
                project,
                sort(Sort.by(Sort.Direction.ASC, "month")));

        return mongoTemplate
                .aggregate(aggregation, Order.class, MonthlyAggregation.class)
                .getMappedResults();
    }

    /**
     * Shared product pipeline:
     * match -> unwind items -> [seller item match] -> group by product ->
     * project -> sort -> limit.
     */
    private List<ProductAggregation> productAggregation(
            Criteria criteria,
            String sellerId,
            Sort.Direction direction,
            String sortField) {

        GroupOperation group = group("items.productId")
                .first("items.name").as("name")
                .first("items.imageUrl").as("imageUrl")
                .sum(toDecimal("items.quantity")).as("quantity")
                .sum(itemAmount()).as("amountSpent");

        ProjectionOperation project = project()
                .and("_id").as("productId")
                .and("name").as("name")
                .and("imageUrl").as("imageUrl")
                .and("quantity").as("quantity")
                .and("amountSpent").as("amountSpent")
                .andExclude("_id");

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(match(criteria));
        operations.add(unwind("items"));
        if (sellerId != null) {
            operations.add(match(Criteria.where("items.sellerId").is(sellerId)));
        }
        operations.add(group);
        operations.add(project);
        operations.add(sort(Sort.by(direction, sortField)));
        operations.add(limit(OrderStatsConstants.TOP_LIMIT));

        Aggregation aggregation = newAggregation(
                operations.toArray(AggregationOperation[]::new));

        return mongoTemplate
                .aggregate(aggregation, Order.class, ProductAggregation.class)
                .getMappedResults();
    }

    private Criteria revenueCriteria(String ownerField, String ownerId) {
        return Criteria.where(ownerField).is(ownerId)
                .and("status").is(OrderStatsConstants.REVENUE_STATUS);
    }

    /**
     * MongoDB does not coerce strings inside arithmetic operators. Some older
     * order documents store money/quantity values as strings, which made
     * {@code $multiply} fail with TypeMismatch. Convert explicitly and fall
     * back to zero for malformed values instead of failing the whole dashboard.
     */
    private ArithmeticOperators.Multiply itemAmount() {
        return ArithmeticOperators.Multiply.valueOf(toDecimal("items.price"))
                .multiplyBy(toDecimal("items.quantity"));
    }

    private AggregationExpression toDecimal(String fieldReference) {
        return ConvertOperators.Convert.convertValueOf(fieldReference)
                .to("decimal")
                .onErrorReturn(0)
                .onNullReturn(0);
    }
}
