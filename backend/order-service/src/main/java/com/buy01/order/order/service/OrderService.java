package com.buy01.order.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buy01.order.cart.client.ProductClient;
import com.buy01.order.cart.client.ProductResponse;
import com.buy01.order.common.Role;
import com.buy01.order.common.Status;
import com.buy01.order.order.dto.BuyNowRequest;
import com.buy01.order.order.dto.CreateOrderRequest;
import com.buy01.order.order.dto.OrderItemRequest;
import com.buy01.order.order.dto.OrderQuery;
import com.buy01.order.order.dto.OrderResponse;
import com.buy01.order.order.dto.PageResponse;
import com.buy01.order.order.dto.UpdateOrderStatusRequest;
import com.buy01.order.order.exception.InvalidOrderException;
import com.buy01.order.order.exception.InvalidOrderQueryException;
import com.buy01.order.order.exception.InvalidOrderStateException;
import com.buy01.order.order.exception.OrderAccessDeniedException;
import com.buy01.order.order.exception.OrderNotFoundException;
import com.buy01.order.order.model.entity.DeliveryAddress;
import com.buy01.order.order.messaging.OrderStockEventPublisher;
import com.buy01.order.order.model.entity.Order;
import com.buy01.order.order.model.entity.OrderItem;
import com.buy01.order.order.model.repository.OrderRepository;
import com.buy01.order.security.JwtAuthenticationFilter.UserPrincipal;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_LENGTH = 200;
    private static final Map<Status, Status> NEXT_STATUS = Map.of(
            Status.PENDING, Status.CONFIRMED,
            Status.CONFIRMED, Status.SHIPPED,
            Status.SHIPPED, Status.DELIVERED);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderStockEventPublisher stockEventPublisher;

    public OrderResponse createOrder(UserPrincipal actor, CreateOrderRequest request) {
        return createOrderForBuyer(actor.id(), request);
    }

    public OrderResponse createOrderForBuyer(UUID buyerId, CreateOrderRequest request) {
        if (buyerId == null) {
            throw new OrderAccessDeniedException("Authentication required");
        }
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        String sellerId = resolveSellerId(request);
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : request.items()) {
            items.add(toOrderItem(itemRequest, sellerId));
        }

        return saveOrder(buyerId, trimToNull(request.checkoutId()), sellerId, items, request.address());
    }

    public OrderResponse buyNow(UserPrincipal actor, BuyNowRequest request) {
        if (request == null || !StringUtils.hasText(request.productId())) {
            throw new InvalidOrderException("productId is required");
        }
        if (request.quantity() < 1) {
            throw new InvalidOrderException("quantity must be at least 1");
        }
        if (request.address() == null) {
            throw new InvalidOrderException("Delivery address is required");
        }

        ProductResponse product = fetchProduct(request.productId().trim());
        if (product.price() == null) {
            throw new InvalidOrderException("Product has no price");
        }
        if (request.quantity() > product.quantity()) {
            throw new InvalidOrderException(
                    "Insufficient stock for product " + product.id()
                            + ": requested " + request.quantity()
                            + ", available " + product.quantity());
        }

        String sellerId = trimToNull(product.userId());
        if (sellerId == null) {
            throw new InvalidOrderException("Product seller is unavailable");
        }
        if (!StringUtils.hasText(product.name())) {
            throw new InvalidOrderException("Product name is unavailable");
        }

        String imageUrl = product.imagesIds() == null || product.imagesIds().isEmpty()
                ? null
                : trimToNull(product.imagesIds().getFirst());

        OrderItem item = OrderItem.builder()
                .productId(product.id() == null ? request.productId().trim() : product.id())
                .sellerId(sellerId)
                .name(product.name().trim())
                .imageUrl(imageUrl)
                .price(product.price())
                .quantity(request.quantity())
                .build();

        return saveOrder(actor.id(), null, sellerId, List.of(item), request.address());
    }

    public PageResponse<OrderResponse> getBuyerOrders(UserPrincipal actor, OrderQuery query) {
        Pageable pageable = toPageable(query);

        Page<Order> page = orderRepository.search(
                actor.id().toString(),
                null,
                trimToNull(query.search()),
                query.status(),
                startOfDay(query.from()),
                startOfDayExclusive(query.to()),
                pageable);

        return PageResponse.of(page, OrderResponse::from);
    }

    public PageResponse<OrderResponse> getSellerOrders(UserPrincipal actor, OrderQuery query) {
        requireSeller(actor);
        Pageable pageable = toPageable(query);

        Page<Order> page = orderRepository.search(
                null,
                actor.id().toString(),
                trimToNull(query.search()),
                query.status(),
                startOfDay(query.from()),
                startOfDayExclusive(query.to()),
                pageable);

        return PageResponse.of(page, OrderResponse::from);
    }

    public OrderResponse getOrder(UserPrincipal actor, String orderId) {
        Order order = findOrder(orderId);
        requireViewAccess(actor, order);
        return OrderResponse.from(order);
    }

    public OrderResponse cancelOrder(UserPrincipal actor, String orderId) {
        Order order = findOrder(orderId);

        boolean buyer = isBuyer(actor, order);
        boolean seller = isSeller(actor, order);
        if (!buyer && !seller) {
            throw new OrderAccessDeniedException("You do not have access to this order");
        }

        if (order.getStatus() != Status.PENDING && order.getStatus() != Status.CONFIRMED) {
            throw new InvalidOrderStateException("Only pending or confirmed orders can be cancelled");
        }

        order.setStatus(Status.CANCELLED);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);
        stockEventPublisher.releaseStock(saved);
        return OrderResponse.from(saved);
    }

    public OrderResponse redoOrder(UserPrincipal actor, String orderId) {
        Order order = findOrder(orderId);

        if (!isBuyer(actor, order)) {
            throw new OrderAccessDeniedException("Only the buyer can redo this order");
        }

        if (order.getStatus() != Status.CANCELLED && order.getStatus() != Status.DELIVERED) {
            throw new InvalidOrderStateException("Only cancelled or delivered orders can be redone");
        }

        Instant now = Instant.now();
        Order redo = Order.builder()
                .checkoutId(order.getCheckoutId())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .status(Status.PENDING)
                .items(copyItems(order.getItems()))
                .total(order.getTotal())
                .address(order.getAddress())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Order saved = orderRepository.save(redo);
        stockEventPublisher.reserveStock(saved);
        return OrderResponse.from(saved);
    }

    public OrderResponse updateStatus(UserPrincipal actor, String orderId, UpdateOrderStatusRequest request) {
        requireSeller(actor);
        Order order = findOrder(orderId);

        if (!isSellerOf(actor.id(), order)) {
            throw new OrderAccessDeniedException("Only the seller of this order can update its status");
        }

        Status target = request == null ? null : request.status();
        if (target == null) {
            throw new InvalidOrderException("status is required");
        }

        Status current = order.getStatus();
        Status expected = current == null ? null : NEXT_STATUS.get(current);
        if (expected == null || expected != target) {
            throw new InvalidOrderStateException(
                    "Cannot change order status from " + current + " to " + target);
        }

        order.setStatus(target);
        order.setUpdatedAt(Instant.now());
        return OrderResponse.from(orderRepository.save(order));
    }

    public void deleteOrder(UserPrincipal actor, String orderId) {
        Order order = findOrder(orderId);

        if (!isBuyer(actor, order)) {
            throw new OrderAccessDeniedException("Only the buyer can remove this order");
        }

        if (order.getStatus() != Status.CANCELLED && order.getStatus() != Status.DELIVERED) {
            throw new InvalidOrderStateException("Only cancelled or delivered orders can be removed");
        }

        orderRepository.delete(order);
    }

    private OrderResponse saveOrder(
            UUID buyerId,
            String checkoutId,
            String sellerId,
            List<OrderItem> items,
            DeliveryAddress address) {

        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant now = Instant.now();
        Order order = Order.builder()
                .checkoutId(checkoutId)
                .buyerId(buyerId.toString())
                .sellerId(sellerId)
                .status(Status.PENDING)
                .items(items)
                .total(total)
                .address(address)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Order saved = orderRepository.save(order);
        stockEventPublisher.reserveStock(saved);
        return OrderResponse.from(saved);
    }

    private ProductResponse fetchProduct(String productId) {
        try {
            ProductResponse product = productClient.getProduct(productId);
            if (product == null) {
                throw new InvalidOrderException("Product not found: " + productId);
            }
            return product;
        } catch (FeignException.NotFound e) {
            throw new InvalidOrderException("Product not found: " + productId);
        } catch (FeignException e) {
            throw new InvalidOrderException("Unable to reach product service");
        }
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void requireViewAccess(UserPrincipal actor, Order order) {
        if (!isBuyer(actor, order) && !isSeller(actor, order)) {
            throw new OrderAccessDeniedException("You do not have access to this order");
        }
    }

    private boolean isBuyer(UserPrincipal actor, Order order) {
        return Objects.equals(order.getBuyerId(), actor.id().toString());
    }

    private boolean isSeller(UserPrincipal actor, Order order) {
        return actor.role() == Role.SELLER && isSellerOf(actor.id(), order);
    }

    private boolean isSellerOf(UUID actorId, Order order) {
        String id = actorId.toString();
        if (Objects.equals(order.getSellerId(), id)) {
            return true;
        }
        return order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> Objects.equals(item.getSellerId(), id));
    }

    private void requireSeller(UserPrincipal actor) {
        if (actor.role() != Role.SELLER) {
            throw new OrderAccessDeniedException("Only sellers can perform this action");
        }
    }

    private String resolveSellerId(CreateOrderRequest request) {
        String sellerId = trimToNull(request.sellerId());

        for (OrderItemRequest item : request.items()) {
            if (item == null) {
                throw new InvalidOrderException("Order items cannot contain null entries");
            }

            String itemSellerId = trimToNull(item.sellerId());
            if (itemSellerId == null) {
                continue;
            }

            if (sellerId == null) {
                sellerId = itemSellerId;
            } else if (!sellerId.equals(itemSellerId)) {
                throw new InvalidOrderException("All order items must belong to the same seller");
            }
        }

        if (sellerId == null) {
            throw new InvalidOrderException("sellerId is required for the order or each item");
        }

        return sellerId;
    }

    private OrderItem toOrderItem(OrderItemRequest item, String sellerId) {
        if (item == null) {
            throw new InvalidOrderException("Order items cannot contain null entries");
        }
        if (!StringUtils.hasText(item.productId())) {
            throw new InvalidOrderException("productId is required for each order item");
        }
        if (!StringUtils.hasText(item.name())) {
            throw new InvalidOrderException("name is required for each order item");
        }
        if (item.price() == null) {
            throw new InvalidOrderException("price is required for each order item");
        }
        if (item.price().signum() < 0) {
            throw new InvalidOrderException("price must be zero or positive");
        }
        if (item.quantity() < 1) {
            throw new InvalidOrderException("quantity must be at least 1");
        }

        String itemSellerId = trimToNull(item.sellerId());
        if (itemSellerId != null && !itemSellerId.equals(sellerId)) {
            throw new InvalidOrderException("All order items must belong to the same seller");
        }

        return OrderItem.builder()
                .productId(item.productId().trim())
                .sellerId(sellerId)
                .name(item.name().trim())
                .imageUrl(trimToNull(item.imageUrl()))
                .price(item.price())
                .quantity(item.quantity())
                .build();
    }

    private List<OrderItem> copyItems(List<OrderItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(item -> OrderItem.builder()
                        .productId(item.getProductId())
                        .sellerId(item.getSellerId())
                        .name(item.getName())
                        .imageUrl(item.getImageUrl())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private Pageable toPageable(OrderQuery query) {
        int page = query.page();
        int size = query.size();
        List<String> details = new ArrayList<>();

        if (page < 0) {
            details.add("page must be zero or positive");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            details.add("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            details.add("from must be on or before to");
        }
        String search = trimToNull(query.search());
        if (search != null && search.length() > MAX_SEARCH_LENGTH) {
            details.add("search must be at most " + MAX_SEARCH_LENGTH + " characters");
        }

        if (!details.isEmpty()) {
            throw new InvalidOrderQueryException(details);
        }

        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant startOfDayExclusive(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
