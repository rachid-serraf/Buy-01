package com.buy01.order.order.stats.controller

import java.math.BigDecimal
import java.util.UUID

import com.buy01.order.common.Role
import com.buy01.order.order.stats.dto.BuyerStatsResponse
import com.buy01.order.order.stats.dto.SellerStatsResponse
import com.buy01.order.order.stats.service.OrderStatsService
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal

import spock.lang.Specification

class OrderStatsControllerSpec extends Specification {

    private OrderStatsService statsService
    private OrderStatsController controller

    def setup() {
        statsService = Mock()
        controller = new OrderStatsController(statsService)
    }

    def "buyer stats delegates the authenticated principal"() {
        given:
        def principal = new UserPrincipal(UUID.randomUUID(), "buyer@example.com", Role.CLIENT)
        def response = BuyerStatsResponse.builder()
                .totalSpent(BigDecimal.TEN)
                .ordersCount(1L)
                .mostBoughtProducts([])
                .bestProducts([])
                .topCategories([])
                .build()

        when:
        def result = controller.getBuyerStats(principal)

        then:
        1 * statsService.getBuyerStats(principal) >> response
        result == response
    }

    def "seller stats delegates the authenticated principal"() {
        given:
        def principal = new UserPrincipal(UUID.randomUUID(), "seller@example.com", Role.SELLER)
        def response = SellerStatsResponse.builder()
                .totalRevenue(BigDecimal.TEN)
                .ordersCount(1L)
                .unitsSold(2L)
                .bestSellingProducts([])
                .revenueByMonth([])
                .build()

        when:
        def result = controller.getSellerStats(principal)

        then:
        1 * statsService.getSellerStats(principal) >> response
        result == response
    }
}
