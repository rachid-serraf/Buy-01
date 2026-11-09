package com.buy01.order.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.buy01.order.cart.dto.CartItemDTO;
import com.buy01.order.cart.model.entity.Cart;
import com.buy01.order.cart.service.CartService;
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<Cart> addToCart(
            @Valid @RequestBody CartItemDTO.AddRequest request, @AuthenticationPrincipal UserPrincipal principal) {

        Cart cart = cartService.addItem(principal.id(), request);
        // TODO: mapper Cart to CartResponse
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/items")
    public ResponseEntity<Cart> getCartItems(@AuthenticationPrincipal UserPrincipal principal){
        Cart cart = cartService.findByUserId(principal.id());
        return ResponseEntity.ok(cart);
    }
}
