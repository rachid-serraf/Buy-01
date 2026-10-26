package com.buy01.product.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buy01.product.domain.model.Product;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MongoProductAdapterTest {

    @Mock
    private SpringDataProductRepository springRepository;

    @Mock
    private ProductDocumentMapper mapper;

    private MongoProductAdapter adapter;

    private final Product domain = product("prod-1");
    private final ProductDocument document = document("prod-1");

    @BeforeEach
    void setUp() {
        adapter = new MongoProductAdapter(springRepository, mapper);
    }

    @Test
    void saveMapsDocumentAndReturnsDomain() {
        when(mapper.toDocument(domain)).thenReturn(document);
        when(springRepository.save(document)).thenReturn(Mono.just(document));
        when(mapper.toDomain(document)).thenReturn(domain);

        StepVerifier.create(adapter.save(domain))
                .assertNext(saved -> assertThat(saved).isSameAs(domain))
                .verifyComplete();

        verify(springRepository).save(document);
    }

    @Test
    void findByIdMapsDocument() {
        when(springRepository.findById("prod-1")).thenReturn(Mono.just(document));
        when(mapper.toDomain(document)).thenReturn(domain);

        StepVerifier.create(adapter.findById("prod-1"))
                .assertNext(found -> assertThat(found).isSameAs(domain))
                .verifyComplete();
    }

    @Test
    void findByUserIdMapsDocuments() {
        when(springRepository.findByUserId("seller-1")).thenReturn(Flux.just(document));
        when(mapper.toDomain(document)).thenReturn(domain);

        StepVerifier.create(adapter.findByUserId("seller-1"))
                .assertNext(found -> assertThat(found).isSameAs(domain))
                .verifyComplete();
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        when(springRepository.deleteById("prod-1")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.deleteById("prod-1"))
                .verifyComplete();

        verify(springRepository).deleteById("prod-1");
    }

    @Test
    void findTop10ByCreatedAtMapsDocuments() {
        Instant before = Instant.parse("2026-03-25T10:00:00Z");
        when(springRepository.findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(before))
                .thenReturn(Flux.just(document));
        when(mapper.toDomain(document)).thenReturn(domain);

        StepVerifier.create(adapter.findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(before))
                .assertNext(found -> assertThat(found).isSameAs(domain))
                .verifyComplete();
    }

    @Test
    void findTop10ByUserIdAndCreatedAtMapsDocuments() {
        Instant before = Instant.parse("2026-03-25T10:00:00Z");
        when(springRepository.findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc("seller-1", before))
                .thenReturn(Flux.just(document));
        when(mapper.toDomain(document)).thenReturn(domain);

        StepVerifier.create(adapter.findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc("seller-1", before))
                .assertNext(found -> assertThat(found).isSameAs(domain))
                .verifyComplete();
    }

    private Product product(String id) {
        return Product.builder()
                .id(id)
                .name("Product")
                .description("A product description")
                .price(10.0)
                .quantity(1)
                .userId("seller-1")
                .createdAt(Instant.parse("2026-03-25T10:00:00Z"))
                .updatedAt(Instant.parse("2026-03-25T10:00:00Z"))
                .build();
    }

    private ProductDocument document(String id) {
        ProductDocument doc = new ProductDocument();
        doc.setId(id);
        return doc;
    }
}
