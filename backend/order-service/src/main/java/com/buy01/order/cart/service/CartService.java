package com.buy01.order.cart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.order.cart.client.ProductClient;
import com.buy01.order.cart.client.ProductResponse;
import com.buy01.order.cart.dto.CartIssueType;
import com.buy01.order.cart.dto.CartItemDTO;
import com.buy01.order.cart.dto.CartItemIssue;
import com.buy01.order.cart.dto.CheckoutResponse;
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

        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

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

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(request.quantity());
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

    public Cart removeItem(UUID userId, String productId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        Cart cart = findByUserId(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not in cart: " + productId);
        }

        return cartRepository.save(cart);
    }

    public CheckoutResponse checkoutItems(UUID userId) {

        Cart cart = findByUserId(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        List<CartItemIssue> issues = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductResponse product;
            try {
                product = fetchProduct(item.getProductId());
            } catch (ResponseStatusException e) {
                if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                    issues.add(new CartItemIssue(
                            item.getProductId(),
                            item.getName(),
                            CartIssueType.PRODUCT_UNAVAILABLE,
                            "This product is no longer available"));
                    continue;
                }
                throw e;
            }

            if (item.getQuantity() > product.quantity()) {
                issues.add(new CartItemIssue(
                        item.getProductId(),
                        item.getName(),
                        CartIssueType.INSUFFICIENT_STOCK,
                        "Only " + product.quantity() + " left in stock, but "
                                + item.getQuantity() + " requested"));
            }

            if (priceChanged(item.getUnitPrice(), product.price())) {
                issues.add(new CartItemIssue(
                        item.getProductId(),
                        item.getName(),
                        CartIssueType.PRICE_CHANGED,
                        "Price changed from " + item.getUnitPrice()
                                + " to " + product.price()));
                item.setUnitPrice(product.price());
            }

            if (!Objects.equals(item.getName(), product.name())) {
                issues.add(new CartItemIssue(
                        item.getProductId(),
                        item.getName(),
                        CartIssueType.NAME_CHANGED,
                        "Product name changed to " + product.name()));
                item.setName(product.name());
            }

            if (!Objects.equals(item.getSellerId(), product.userId())) {
                issues.add(new CartItemIssue(
                        item.getProductId(),
                        item.getName(),
                        CartIssueType.SELLER_CHANGED,
                        "This product is now sold by another seller"));
                item.setSellerId(product.userId());
            }

            String imageId = firstImage(product);
            if (!Objects.equals(item.getImageId(), imageId)) {
                issues.add(new CartItemIssue(
                        item.getProductId(),
                        item.getName(),
                        CartIssueType.IMAGE_CHANGED,
                        "The product image has changed"));
                item.setImageId(imageId);
            }
        }

        if (!issues.isEmpty()) {
            cartRepository.save(cart);
        }

        return new CheckoutResponse(issues.isEmpty(), issues, cart);
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

    private boolean priceChanged(BigDecimal cartPrice, BigDecimal productPrice) {
        if (cartPrice == null || productPrice == null) {
            return !Objects.equals(cartPrice, productPrice);
        }
        return cartPrice.compareTo(productPrice) != 0;
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
