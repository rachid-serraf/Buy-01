package com.buy01.order.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.buy01.order.common.Status;
import com.buy01.order.order.model.entity.Order;
import com.buy01.order.order.model.repository.OrderRepository;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderService = new OrderService(orderRepository);
    }

    private static Order newOrder() {
        return new Order(null, null, List.of(), null, Status.PENDING);
    }

    @Test
    void createOrderSavesOrder() {
        Order order = newOrder();

        orderService.createOrder(order);

        verify(orderRepository).save(order);
    }

    @Test
    void getOrdersReturnsRepositoryResult() {
        List<Order> orders = List.of(newOrder());
        when(orderRepository.findAll()).thenReturn(orders);

        assertEquals(orders, orderService.getOrders());
    }

    @Test
    void getOrderByIdReturnsRepositoryResult() {
        Order order = newOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertEquals(Optional.of(order), orderService.getOrderById("order-1"));
    }

    @Test
    void getOrderByIdReturnsEmptyWhenMissing() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertTrue(orderService.getOrderById("missing").isEmpty());
    }

    @Test
    void deleteOrderByIdDelegatesToRepository() {
        orderService.deleteOrderById("order-1");

        verify(orderRepository).deleteById("order-1");
    }

    @Test
    void updateOrderByIdSetsIdAndSaves() {
        Order order = newOrder();

        orderService.updateOrderById("order-1", order);

        assertEquals("order-1", order.getId());
        verify(orderRepository).save(order);
    }
}
