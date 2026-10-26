package com.buy01.product.infrastructure.messaging;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buy01.product.application.events.UserDeletedEvent;
import com.buy01.product.application.service.ProductServiceImpl;
import com.buy01.product.domain.model.Product;
import com.buy01.product.domain.ports.outbound.ProductRepositoryPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserListenerTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private ProductServiceImpl productService;

    private UserListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserListener(productRepository, productService);
    }

    @Test
    void deletesAllProductsForDeletedUser() {
        Product first = product("prod-1");
        Product second = product("prod-2");
        when(productRepository.findByUserId("user-1")).thenReturn(Flux.just(first, second));
        when(productService.deleteProduct("prod-1", "user-1")).thenReturn(Mono.empty());
        when(productService.deleteProduct("prod-2", "user-1")).thenReturn(Mono.empty());

        listener.onUserDeleted(new UserDeletedEvent("user-1"));

        verify(productService, timeout(1000)).deleteProduct("prod-1", "user-1");
        verify(productService, timeout(1000)).deleteProduct("prod-2", "user-1");
    }

    @Test
    void handlesEmptyProductList() {
        when(productRepository.findByUserId("user-1")).thenReturn(Flux.empty());

        listener.onUserDeleted(new UserDeletedEvent("user-1"));

        verify(productRepository, timeout(1000)).findByUserId("user-1");
    }

    @Test
    void handlesRepositoryFailureWithoutThrowing() {
        when(productRepository.findByUserId("user-1"))
                .thenReturn(Flux.error(new IllegalStateException("database down")));

        listener.onUserDeleted(new UserDeletedEvent("user-1"));

        verify(productRepository, timeout(1000)).findByUserId("user-1");
    }

    private Product product(String id) {
        return Product.builder()
                .id(id)
                .name("Product")
                .description("A product description")
                .price(10.0)
                .quantity(1)
                .userId("user-1")
                .createdAt(Instant.parse("2026-03-25T10:00:00Z"))
                .updatedAt(Instant.parse("2026-03-25T10:00:00Z"))
                .build();
    }
}
