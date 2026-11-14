package com.buy01.order.order.stats.service

import java.time.YearMonth
import java.time.ZoneOffset

import com.buy01.order.common.Role
import com.buy01.order.order.exception.OrderAccessDeniedException
import com.buy01.order.order.stats.dto.BuyerStatsResponse
import com.buy01.order.order.stats.dto.SellerStatsResponse
import com.buy01.order.order.stats.mapper.OrderStatsMapper
import com.buy01.order.order.stats.model.BuyerSummaryAggregation
import com.buy01.order.order.stats.model.CategoryAggregation
import com.buy01.order.order.stats.model.MonthlyAggregation
import com.buy01.order.order.stats.model.ProductAggregation
import com.buy01.order.order.stats.model.SellerSummaryAggregation
import com.buy01.order.order.stats.repository.OrderStatsRepository
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal

import spock.lang.Specification

class OrderStatsServiceSpec extends Specification {

    private OrderStatsRepository repository
    private OrderStatsService service

    def setup() {
        repository = Mock()
        service = new OrderStatsService(repository, new OrderStatsMapper())
    }

    private static UserPrincipal buyer(String id = UUID.randomUUID().toString()) {
        new UserPrincipal(UUID.fromString(id), "buyer@example.com", Role.CLIENT)
    }

    private static UserPrincipal seller(String id = UUID.randomUUID().toString()) {
        new UserPrincipal(UUID.fromString(id), "seller@example.com", Role.SELLER)
    }

    private static ProductAggregation product(String id, long quantity, String amount) {
        ProductAggregation.builder()
                .productId(id)
                .name("Product " + id)
                .imageUrl("image-" + id)
                .quantity(quantity)
                .amountSpent(new BigDecimal(amount))
                .build()
    }

    def "buyer stats maps summaries and top rows"() {
        given:
        def actor = buyer()
        def buyerId = actor.id().toString()
        def summary = BuyerSummaryAggregation.builder()
                .totalSpent(new BigDecimal("120.50"))
                .ordersCount(3L)
                .build()
        def phone = product("phone", 2L, "19.98")
        def category = CategoryAggregation.builder()
                .category("ELECTRONICS")
                .quantity(2L)
                .amountSpent(new BigDecimal("19.98"))
                .build()

        when:
        BuyerStatsResponse result = service.getBuyerStats(actor)

        then:
        1 * repository.buyerSummary(buyerId) >> summary
        1 * repository.buyerMostBoughtProducts(buyerId) >> [phone]
        1 * repository.buyerBestProducts(buyerId) >> [phone]
        1 * repository.buyerTopCategories(buyerId) >> [category]

        result.totalSpent == new BigDecimal("120.50")
        result.ordersCount == 3L
        result.mostBoughtProducts*.productId == ["phone"]
        result.bestProducts*.amountSpent == [new BigDecimal("19.98")]
        result.topCategories*.category == ["ELECTRONICS"]
    }

    def "buyer with no orders gets zeros and empty lists"() {
        given:
        def actor = buyer()
        def buyerId = actor.id().toString()

        when:
        BuyerStatsResponse result = service.getBuyerStats(actor)

        then:
        1 * repository.buyerSummary(buyerId) >> null
        1 * repository.buyerMostBoughtProducts(buyerId) >> []
        1 * repository.buyerBestProducts(buyerId) >> []
        1 * repository.buyerTopCategories(buyerId) >> []

        result.totalSpent == BigDecimal.ZERO
        result.ordersCount == 0L
        result.mostBoughtProducts == []
        result.bestProducts == []
        result.topCategories == []
    }

    def "seller stats maps totals, products and fills six trailing months"() {
        given:
        def actor = seller()
        def sellerId = actor.id().toString()
        def currentMonth = YearMonth.now(ZoneOffset.UTC)
        def summary = SellerSummaryAggregation.builder()
                .totalRevenue(new BigDecimal("99.90"))
                .ordersCount(4L)
                .unitsSold(6L)
                .build()
        def monthly = MonthlyAggregation.builder()
                .month(currentMonth.toString())
                .revenue(new BigDecimal("49.95"))
                .unitsSold(3L)
                .build()

        when:
        SellerStatsResponse result = service.getSellerStats(actor)

        then:
        1 * repository.sellerSummary(sellerId) >> summary
        1 * repository.sellerBestSellingProducts(sellerId) >> [product("phone", 6L, "99.90")]
        1 * repository.sellerRevenueByMonth(sellerId, _) >> [monthly]

        result.totalRevenue == new BigDecimal("99.90")
        result.ordersCount == 4L
        result.unitsSold == 6L
        result.bestSellingProducts.first().unitsSold == 6L
        result.bestSellingProducts.first().revenue == new BigDecimal("99.90")
        result.revenueByMonth.size() == 6
        result.revenueByMonth.last().month == currentMonth.toString()
        result.revenueByMonth.last().revenue == new BigDecimal("49.95")
        result.revenueByMonth.last().unitsSold == 3L
        result.revenueByMonth.first().revenue == BigDecimal.ZERO
    }

    def "seller with no orders gets zeros and six zero months"() {
        given:
        def actor = seller()
        def sellerId = actor.id().toString()

        when:
        SellerStatsResponse result = service.getSellerStats(actor)

        then:
        1 * repository.sellerSummary(sellerId) >> null
        1 * repository.sellerBestSellingProducts(sellerId) >> []
        1 * repository.sellerRevenueByMonth(sellerId, _) >> []

        result.totalRevenue == BigDecimal.ZERO
        result.ordersCount == 0L
        result.unitsSold == 0L
        result.bestSellingProducts == []
        result.revenueByMonth.size() == 6
        result.revenueByMonth.every { it.revenue == BigDecimal.ZERO && it.unitsSold == 0L }
    }

    def "buyer cannot read seller stats"() {
        when:
        service.getSellerStats(buyer())

        then:
        thrown(OrderAccessDeniedException)
        0 * repository._
    }

    def "seller cannot read buyer stats"() {
        when:
        service.getBuyerStats(seller())

        then:
        thrown(OrderAccessDeniedException)
        0 * repository._
    }

    def "missing actor is forbidden"() {
        when:
        service.getBuyerStats(null)

        then:
        thrown(OrderAccessDeniedException)
        0 * repository._
    }
}
