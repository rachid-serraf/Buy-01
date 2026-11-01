package com.buy01.product.domain.ports.outbound;

import java.time.Instant;

import com.buy01.product.domain.model.Product;

import com.buy01.product.infrastructure.web.dto.ProductSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {

    Mono<Product> save(Product product);

    Mono<Product> findById(String id);

    Flux<Product> findByUserId(String userId);

    Mono<Void> deleteById(String id);

    Flux<Product> findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(Instant lastProduct);

    Flux<Product> findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(String userId, Instant beforeTime);

    Mono<Page<Product>> search(ProductSearchRequest request, Pageable pageable);
}
