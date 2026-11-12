package com.buy01.order.cart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.buy01.order.order.dto.CreateOrderRequest;
import com.buy01.order.order.model.entity.DeliveryAddress;
import com.buy01.order.order.dto.OrderItemRequest;
import com.buy01.order.order.dto.OrderResponse;
import com.buy01.order.order.service.OrderService;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final OrderService orderService;

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

    public CheckoutResponse checkoutItems(UUID userId, DeliveryAddress address) {

        if (address == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivery address is required");
        }

        Cart cart = findByUserId(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        List<CartItemIssue> issues = new ArrayList<>();

        Iterator<CartItem> iterator = cart.getItems().iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            ProductResponse product;
            try {
                product = fetchProduct(item.getProductId());
            } catch (ResponseStatusException e) {
                if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                    issues.add(new CartItemIssue(
                            item.getProductId(),
                            item.getName(),
                            CartIssueType.PRODUCT_UNAVAILABLE,
                            "This product is no longer available and was removed from your cart"));
                    iterator.remove();
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
            return new CheckoutResponse(false, issues, cart, List.of());
        }

        List<OrderResponse> orders = createOrders(userId, cart, address);
        cart.getItems().clear();
        cartRepository.save(cart);

        return new CheckoutResponse(true, List.of(), cart, orders);
    }

    private List<OrderResponse> createOrders(UUID userId, Cart cart, DeliveryAddress address) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Map<String, List<CartItem>> itemsBySeller = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            String sellerId = item.getSellerId();
            if (sellerId == null || sellerId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cart item has no seller: " + item.getProductId());
            }
            itemsBySeller.computeIfAbsent(sellerId, key -> new ArrayList<>()).add(item);
        }

        String checkoutId = UUID.randomUUID().toString();
        List<OrderResponse> orders = new ArrayList<>();

        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<OrderItemRequest> items = entry.getValue().stream()
                    .map(item -> new OrderItemRequest(
                            item.getProductId(),
                            sellerId,
                            item.getName(),
                            item.getImageId(),
                            item.getUnitPrice(),
                            item.getQuantity()))
                    .toList();

            orders.add(orderService.createOrderForBuyer(
                    userId,
                    new CreateOrderRequest(checkoutId, sellerId, items, address)));
        }

        return orders;
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
