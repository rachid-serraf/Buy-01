package com.buy01.order.order.stats.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.order.order.stats.dto.BuyerStatsResponse;
import com.buy01.order.order.stats.dto.SellerStatsResponse;
import com.buy01.order.order.stats.service.OrderStatsService;
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders/stats")
@RequiredArgsConstructor
public class OrderStatsController {

    private final OrderStatsService statsService;

    @GetMapping("/me")
    public BuyerStatsResponse getBuyerStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return statsService.getBuyerStats(principal);
    }

    @GetMapping("/seller")
    public SellerStatsResponse getSellerStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return statsService.getSellerStats(principal);
    }
}
