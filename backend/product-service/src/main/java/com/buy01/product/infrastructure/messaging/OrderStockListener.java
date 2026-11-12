package com.buy01.product.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.buy01.product.domain.ports.outbound.ProductRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockListener {

    private final ProductRepositoryPort productRepository;

    @KafkaListener(
            topics = "${kafka.topics.order-stock:order-stock-events}",
            groupId = "product-service-group",
            containerFactory = "orderStockKafkaListenerContainerFactory")
    public void onOrderStock(OrderStockEvent event) {
        if (event == null || event.action() == null
                || event.items() == null || event.items().isEmpty()) {
            return;
        }

        int sign = event.action() == StockAction.RESERVE ? -1 : 1;

        Flux.fromIterable(event.items())
                .filter(item -> item.productId() != null && !item.productId().isBlank()
                        && item.quantity() > 0)
                .concatMap(item -> productRepository
                        .adjustStock(item.productId(), sign * item.quantity())
                        .doOnNext(product -> log.info(
                                "Kafka: {} {} units for product {} (order {})",
                                event.action(), item.quantity(), item.productId(), event.orderId()))
                        .doOnError(error -> log.error(
                                "Kafka: failed to {} stock for product {} (order {})",
                                event.action(), item.productId(), event.orderId(), error))
                        .onErrorResume(error -> Mono.empty()))
                .then()
                .doOnSuccess(ignored -> log.info(
                        "Kafka: processed {} stock event for order {}",
                        event.action(), event.orderId()))
                .subscribe();
    }
}
