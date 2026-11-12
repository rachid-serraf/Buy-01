package com.buy01.order.order.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.order.common.Status;
import com.buy01.order.order.dto.BuyNowRequest;
import com.buy01.order.order.dto.CreateOrderRequest;
import com.buy01.order.order.dto.OrderQuery;
import com.buy01.order.order.dto.OrderResponse;
import com.buy01.order.order.dto.PageResponse;
import com.buy01.order.order.dto.UpdateOrderStatusRequest;
import com.buy01.order.order.service.OrderService;
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint is working");
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody CreateOrderRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(actor, request));
    }

    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> buyNow(
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody BuyNowRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.buyNow(actor, request));
    }

    @GetMapping("/me")
    public PageResponse<OrderResponse> getMyOrders(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return orderService.getBuyerOrders(
                actor,
                new OrderQuery(search, status, from, to, page, size));
    }

    @GetMapping("/seller")
    public PageResponse<OrderResponse> getSellerOrders(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return orderService.getSellerOrders(
                actor,
                new OrderQuery(search, status, from, to, page, size));
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable String orderId) {

        return orderService.getOrder(actor, orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return orderService.updateStatus(actor, orderId, request);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable String orderId) {

        return orderService.cancelOrder(actor, orderId);
    }

    @PostMapping("/{orderId}/redo")
    public OrderResponse redoOrder(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable String orderId) {

        return orderService.redoOrder(actor, orderId);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable String orderId) {

        orderService.deleteOrder(actor, orderId);
        return ResponseEntity.noContent().build();
    }
}
