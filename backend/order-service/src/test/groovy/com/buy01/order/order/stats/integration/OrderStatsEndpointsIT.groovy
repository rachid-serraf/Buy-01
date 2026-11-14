package com.buy01.order.order.stats.integration

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.time.YearMonth
import java.time.ZoneOffset

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor

import com.buy01.order.common.Role
import com.buy01.order.order.stats.model.BuyerSummaryAggregation
import com.buy01.order.order.stats.model.CategoryAggregation
import com.buy01.order.order.stats.model.MonthlyAggregation
import com.buy01.order.order.stats.model.ProductAggregation
import com.buy01.order.order.stats.model.SellerSummaryAggregation
import com.buy01.order.order.stats.repository.OrderStatsRepository
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal

import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrderStatsEndpointsIT extends Specification {

    @Autowired
    private MockMvc mockMvc

    @MockitoBean
    private OrderStatsRepository statsRepository

    def "GET /orders/stats/me returns buyer sample data"() {
        given:
        def buyerId = UUID.randomUUID().toString()
        def phone = ProductAggregation.builder()
                .productId("product-1")
                .name("Phone")
                .imageUrl("image-1")
                .quantity(2L)
                .amountSpent(new BigDecimal("19.98"))
                .build()
        def category = CategoryAggregation.builder()
                .category("ELECTRONICS")
                .quantity(2L)
                .amountSpent(new BigDecimal("19.98"))
                .build()

        Mockito.when(statsRepository.buyerSummary(buyerId))
                .thenReturn(BuyerSummaryAggregation.builder()
                        .totalSpent(new BigDecimal("120.50"))
                        .ordersCount(3L)
                        .build())
        Mockito.when(statsRepository.buyerMostBoughtProducts(buyerId)).thenReturn([phone])
        Mockito.when(statsRepository.buyerBestProducts(buyerId)).thenReturn([phone])
        Mockito.when(statsRepository.buyerTopCategories(buyerId)).thenReturn([category])

        when:
        def response = mockMvc.perform(get("/orders/stats/me")
                .with(auth(buyerId, Role.CLIENT)))

        then:
        response.andExpect(status().isOk())
                .andExpect(jsonPath('$.totalSpent').value(120.5))
                .andExpect(jsonPath('$.ordersCount').value(3))
                .andExpect(jsonPath('$.mostBoughtProducts.length()').value(1))
                .andExpect(jsonPath('$.mostBoughtProducts[0].productId').value("product-1"))
                .andExpect(jsonPath('$.mostBoughtProducts[0].name').value("Phone"))
                .andExpect(jsonPath('$.mostBoughtProducts[0].quantity').value(2))
                .andExpect(jsonPath('$.mostBoughtProducts[0].amountSpent').value(19.98))
                .andExpect(jsonPath('$.bestProducts[0].productId').value("product-1"))
                .andExpect(jsonPath('$.topCategories[0].category').value("ELECTRONICS"))
    }

    def "GET /orders/stats/seller returns seller sample data and six months"() {
        given:
        def sellerId = UUID.randomUUID().toString()
        def currentMonth = YearMonth.now(ZoneOffset.UTC)
        def firstMonth = currentMonth.minusMonths(5)
        def fromInclusive = firstMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        def phone = ProductAggregation.builder()
                .productId("product-1")
                .name("Phone")
                .imageUrl("image-1")
                .quantity(6L)
                .amountSpent(new BigDecimal("99.90"))
                .build()
        def monthly = MonthlyAggregation.builder()
                .month(currentMonth.toString())
                .revenue(new BigDecimal("49.95"))
                .unitsSold(3L)
                .build()

        Mockito.when(statsRepository.sellerSummary(sellerId))
                .thenReturn(SellerSummaryAggregation.builder()
                        .totalRevenue(new BigDecimal("99.90"))
                        .ordersCount(4L)
                        .unitsSold(6L)
                        .build())
        Mockito.when(statsRepository.sellerBestSellingProducts(sellerId)).thenReturn([phone])
        Mockito.when(statsRepository.sellerRevenueByMonth(sellerId, fromInclusive))
                .thenReturn([monthly])

        when:
        def response = mockMvc.perform(get("/orders/stats/seller")
                .with(auth(sellerId, Role.SELLER)))

        then:
        response.andExpect(status().isOk())
                .andExpect(jsonPath('$.totalRevenue').value(99.9))
                .andExpect(jsonPath('$.ordersCount').value(4))
                .andExpect(jsonPath('$.unitsSold').value(6))
                .andExpect(jsonPath('$.bestSellingProducts[0].productId').value("product-1"))
                .andExpect(jsonPath('$.bestSellingProducts[0].unitsSold').value(6))
                .andExpect(jsonPath('$.bestSellingProducts[0].revenue').value(99.9))
                .andExpect(jsonPath('$.revenueByMonth.length()').value(6))
                .andExpect(jsonPath('$.revenueByMonth[5].month').value(currentMonth.toString()))
                .andExpect(jsonPath('$.revenueByMonth[5].revenue').value(49.95))
    }

    def "buyer is forbidden from the seller endpoint"() {
        given:
        def buyerId = UUID.randomUUID().toString()

        when:
        def response = mockMvc.perform(get("/orders/stats/seller")
                .with(auth(buyerId, Role.CLIENT)))

        then:
        response.andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value("ORDER_ACCESS_DENIED"))
                .andExpect(jsonPath('$.message').value("Only sellers can view seller statistics"))
                .andExpect(jsonPath('$.details').isArray())
    }

    def "seller is forbidden from the buyer endpoint"() {
        given:
        def sellerId = UUID.randomUUID().toString()

        when:
        def response = mockMvc.perform(get("/orders/stats/me")
                .with(auth(sellerId, Role.SELLER)))

        then:
        response.andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value("ORDER_ACCESS_DENIED"))
    }

    private static RequestPostProcessor auth(String userId, Role role) {
        return { request ->
            def principal = new UserPrincipal(UUID.fromString(userId), "user@example.com", role)
            def authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    [new SimpleGrantedAuthority("ROLE_" + role.name())])
            SecurityContextHolder.getContext().setAuthentication(authentication)
            request
        } as RequestPostProcessor
    }
}
