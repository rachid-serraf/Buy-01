package com.buy01.product.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buy01.product.domain.ports.outbound.ProductRepositoryPort;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OrderStockListenerTest {

    @Mock
    private ProductRepositoryPort productRepository;

    private OrderStockListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderStockListener(productRepository);
    }

    @Test
    void reserveSubtractsStockForEachProduct() {
        when(productRepository.adjustStock("prod-1", -2)).thenReturn(Mono.empty());
        when(productRepository.adjustStock("prod-2", -1)).thenReturn(Mono.empty());

        listener.onOrderStock(new OrderStockEvent(
                "order-1",
                StockAction.RESERVE,
                List.of(new OrderStockItem("prod-1", 2), new OrderStockItem("prod-2", 1))));

        verify(productRepository, timeout(1000)).adjustStock("prod-1", -2);
        verify(productRepository, timeout(1000)).adjustStock("prod-2", -1);
    }

    @Test
    void releaseAddsStockForEachProduct() {
        when(productRepository.adjustStock("prod-1", 3)).thenReturn(Mono.empty());

        listener.onOrderStock(new OrderStockEvent(
                "order-1",
                StockAction.RELEASE,
                List.of(new OrderStockItem("prod-1", 3))));

        verify(productRepository, timeout(1000)).adjustStock("prod-1", 3);
    }

    @Test
    void ignoresEventsWithoutItems() {
        listener.onOrderStock(new OrderStockEvent("order-1", StockAction.RESERVE, List.of()));

        verify(productRepository, never()).adjustStock(anyString(), anyInt());
    }
}
