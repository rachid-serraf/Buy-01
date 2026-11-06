package com.buy01.order.order.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.buy01.order.order.model.entity.Order;
import com.buy01.order.order.model.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public void createOrder(Order order) {
        orderRepository.save(order);
    }

    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    public void deleteOrderById(String orderId) {
        orderRepository.deleteById(orderId);
    }

    public void updateOrderById(String orderId, Order order) {
        order.setId(orderId);
        orderRepository.save(order);
    }
}
