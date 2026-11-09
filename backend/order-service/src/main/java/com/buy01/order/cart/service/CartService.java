package com.buy01.order.cart.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.order.cart.client.ProductClient;
import com.buy01.order.cart.client.ProductResponse;
import com.buy01.order.cart.dto.CartItemDTO;
import com.buy01.order.cart.model.entity.Cart;
import com.buy01.order.cart.model.entity.CartItem;
import com.buy01.order.cart.model.repository.CartRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public Cart addItem(UUID userId, CartItemDTO.AddRequest request) {

        ProductResponse product = fetchProduct(request.productId());

        if (request.quantity() > product.quantity()) {
            throw insufficientStock(product, request.quantity());
        }

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return newCart;
        });

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst();

        int requestedQuantity = existingItem
                .map(item -> item.getQuantity() + request.quantity())
                .orElse(request.quantity());

        if (requestedQuantity > product.quantity()) {
            throw insufficientStock(product, requestedQuantity);
        }

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(requestedQuantity);
            item.setName(product.name());
            item.setUnitPrice(product.price());
            item.setSellerId(product.userId());
            item.setImageId(firstImage(product));
        } else {
            cart.getItems().add(new CartItem(
                    product.id(),
                    product.userId(),
                    product.name(),
                    product.price(),
                    firstImage(product),
                    request.quantity()));
        }

        return cartRepository.save(cart);
    }

    public Cart findByUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return cartRepository.findByUserId(userId).orElse(null);
    }

    private ProductResponse fetchProduct(String productId) {
        try {
            ProductResponse product = productClient.getProduct(productId);
            if (product == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found: " + productId);
            }
            return product;
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found: " + productId);
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach product service");
        }
    }

    private String firstImage(ProductResponse product) {
        List<String> images = product.imagesIds();
        return images == null || images.isEmpty() ? null : images.getFirst();
    }

    private ResponseStatusException insufficientStock(ProductResponse product, int requestedQuantity) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Insufficient stock for product " + product.id()
                        + ": requested " + requestedQuantity
                        + ", available " + product.quantity());
    }
}
