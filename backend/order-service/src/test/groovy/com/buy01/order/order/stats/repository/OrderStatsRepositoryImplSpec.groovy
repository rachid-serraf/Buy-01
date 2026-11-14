package com.buy01.order.order.stats.repository

import com.buy01.order.order.model.entity.Order
import com.buy01.order.order.stats.model.BuyerSummaryAggregation
import com.buy01.order.order.stats.model.CategoryAggregation
import com.buy01.order.order.stats.model.MonthlyAggregation
import com.buy01.order.order.stats.model.ProductAggregation
import com.buy01.order.order.stats.model.SellerSummaryAggregation;
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class OrderStatsRepositoryImplSpec extends Specification {

    MongoTemplate mongoTemplate = Mock()
    
    @Subject
    OrderStatsRepositoryImpl repository = new OrderStatsRepositoryImpl(mongoTemplate)

    def "buyerSummary should build aggregation and return unique result"() {
        given:
        def results = Mock(AggregationResults)
        def expected = new BuyerSummaryAggregation()

        when:
        def res = repository.buyerSummary("buyer-1")

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, BuyerSummaryAggregation.class) >> results
        1 * results.getUniqueMappedResult() >> expected
        res == expected
    }

    def "buyerMostBoughtProducts should call productAggregation with null sellerId"() {
        given:
        def results = Mock(AggregationResults)
        def expectedList = [new ProductAggregation()]

        when:
        def res = repository.buyerMostBoughtProducts("buyer-1")

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, ProductAggregation.class) >> results
        1 * results.getMappedResults() >> expectedList
        res == expectedList
    }

    def "buyerBestProducts should execute correctly"() {
        given:
        def results = Mock(AggregationResults)
        def expectedList = [new ProductAggregation()]

        when:
        def res = repository.buyerBestProducts("buyer-1")

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, ProductAggregation.class) >> results
        1 * results.getMappedResults() >> expectedList
        res == expectedList
    }

    def "buyerTopCategories should execute projection and grouping aggregation"() {
        given:
        def results = Mock(AggregationResults)
        def expectedList = [new CategoryAggregation()]

        when:
        def res = repository.buyerTopCategories("buyer-1")

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, CategoryAggregation.class) >> results
        1 * results.getMappedResults() >> expectedList
        res == expectedList
    }

    def "sellerSummary should execute multi-group aggregation"() {
        given:
        def results = Mock(AggregationResults)
        def expected = new SellerSummaryAggregation()

        when:
        def res = repository.sellerSummary("seller-1")

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, SellerSummaryAggregation.class) >> results
        1 * results.getUniqueMappedResult() >> expected
        res == expected
    }

    def "sellerBestSellingProducts should call productAggregation with non-null sellerId"() {
        given:
        def results = Mock(AggregationResults)
        def expectedList = [new ProductAggregation()]

        when:
        def res = repository.sellerBestSellingProducts("seller-1")

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, ProductAggregation.class) >> results
        1 * results.getMappedResults() >> expectedList
        res == expectedList
    }

    def "sellerRevenueByMonth should build monthly aggregation pipeline"() {
        given:
        def results = Mock(AggregationResults)
        def expectedList = [new MonthlyAggregation()]
        def from = Instant.now()

        when:
        def res = repository.sellerRevenueByMonth("seller-1", from)

        then:
        1 * mongoTemplate.aggregate(_ as Aggregation, Order.class, MonthlyAggregation.class) >> results
        1 * results.getMappedResults() >> expectedList
        res == expectedList
    }
}