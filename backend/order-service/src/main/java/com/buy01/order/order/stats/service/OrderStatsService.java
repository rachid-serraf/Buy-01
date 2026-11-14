package com.buy01.order.order.stats.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;

import com.buy01.order.common.Role;
import com.buy01.order.order.exception.OrderAccessDeniedException;
import com.buy01.order.order.stats.OrderStatsConstants;
import com.buy01.order.order.stats.dto.BuyerStatsResponse;
import com.buy01.order.order.stats.dto.SellerStatsResponse;
import com.buy01.order.order.stats.mapper.OrderStatsMapper;
import com.buy01.order.order.stats.repository.OrderStatsRepository;
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderStatsService {

    private final OrderStatsRepository statsRepository;
    private final OrderStatsMapper statsMapper;

    public BuyerStatsResponse getBuyerStats(UserPrincipal actor) {
        requireRole(actor, Role.CLIENT, "Only buyers can view buyer statistics");

        String buyerId = actor.id().toString();
        return statsMapper.toBuyerStats(
                statsRepository.buyerSummary(buyerId),
                statsRepository.buyerMostBoughtProducts(buyerId),
                statsRepository.buyerBestProducts(buyerId),
                statsRepository.buyerTopCategories(buyerId));
    }

    public SellerStatsResponse getSellerStats(UserPrincipal actor) {
        requireRole(actor, Role.SELLER, "Only sellers can view seller statistics");

        String sellerId = actor.id().toString();
        YearMonth firstMonth = YearMonth.now(ZoneOffset.UTC)
                .minusMonths(OrderStatsConstants.REVENUE_MONTHS - 1L);
        Instant fromInclusive = firstMonth.atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        return statsMapper.toSellerStats(
                statsRepository.sellerSummary(sellerId),
                statsRepository.sellerBestSellingProducts(sellerId),
                statsRepository.sellerRevenueByMonth(sellerId, fromInclusive),
                firstMonth,
                OrderStatsConstants.REVENUE_MONTHS);
    }

    private void requireRole(UserPrincipal actor, Role expected, String message) {
        if (actor == null || actor.id() == null || actor.role() != expected) {
            throw new OrderAccessDeniedException(message);
        }
    }
}
