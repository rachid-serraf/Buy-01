package com.buy01.product.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Query;

import com.buy01.product.domain.model.Product;
import com.buy01.product.infrastructure.web.dto.ProductSearchRequest;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MongoProductAdapterTest {

    @Mock
    private SpringDataProductRepository springRepository;

    @Mock
    private ProductDocumentMapper mapper;

    @Mock
    private ReactiveMongoTemplate template;


    private MongoProductAdapter adapter;

    private final Product domain = product("prod-1");
    private final ProductDocument document = document("prod-1");

    @BeforeEach
    void setUp() {
        adapter = new MongoProductAdapter(springRepository, mapper, template);
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

    @Test
    void searchBuildsTextCategoryAndPriceFiltersAndReturnsAPage() {
        ProductSearchRequest request = new ProductSearchRequest(
                " Phone.* ", " ELECTRONICS ", 100.0, 500.0, "price_asc", 1, 5);
        PageRequest pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "price"));
        when(template.find(any(Query.class), eq(Product.class))).thenReturn(Flux.just(domain));
        when(template.count(any(Query.class), eq(Product.class))).thenReturn(Mono.just(6L));

        StepVerifier.create(adapter.search(request, pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).containsExactly(domain);
                    assertThat(page.getTotalElements()).isEqualTo(6);
                    assertThat(page.getNumber()).isEqualTo(1);
                    assertThat(page.getSize()).isEqualTo(5);
                })
                .verifyComplete();

        ArgumentCaptor<Query> findQuery = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> countQuery = ArgumentCaptor.forClass(Query.class);
        verify(template).find(findQuery.capture(), eq(Product.class));
        verify(template).count(countQuery.capture(), eq(Product.class));

        String filters = countQuery.getValue().getQueryObject().toJson();
        assertThat(filters).contains("name", "description", "Phone.*", "category", "electronics", "price", "$gte", "$lte");
        assertThat(findQuery.getValue().getSkip()).isEqualTo(5);
        assertThat(findQuery.getValue().getLimit()).isEqualTo(5);
        assertThat(findQuery.getValue().getSortObject().toJson()).contains("price", "1");
    }

    @Test
    void searchWithoutFiltersUsesAnEmptyQuery() {
        ProductSearchRequest request = new ProductSearchRequest(" ", " ", null, null, "newest", 0, 12);
        PageRequest pageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(template.find(any(Query.class), eq(Product.class))).thenReturn(Flux.empty());
        when(template.count(any(Query.class), eq(Product.class))).thenReturn(Mono.just(0L));

        StepVerifier.create(adapter.search(request, pageable))
                .assertNext(page -> {
                    assertThat(page).isEmpty();
                    assertThat(page.getTotalElements()).isZero();
                })
                .verifyComplete();

        ArgumentCaptor<Query> countQuery = ArgumentCaptor.forClass(Query.class);
        verify(template).count(countQuery.capture(), eq(Product.class));
        assertThat(countQuery.getValue().getQueryObject()).isEmpty();
    }

    @Test
    void searchSupportsEitherPriceBoundaryWhenTextAndCategoryAreAbsent() {
        PageRequest pageable = PageRequest.of(0, 12);
        when(template.find(any(Query.class), eq(Product.class))).thenReturn(Flux.empty());
        when(template.count(any(Query.class), eq(Product.class))).thenReturn(Mono.just(0L));

        StepVerifier.create(adapter.search(
                        new ProductSearchRequest(null, null, 100.0, null, "newest", 0, 12), pageable))
                .assertNext(page -> assertThat(page).isEmpty())
                .verifyComplete();
        StepVerifier.create(adapter.search(
                        new ProductSearchRequest(null, null, null, 500.0, "newest", 0, 12), pageable))
                .assertNext(page -> assertThat(page).isEmpty())
                .verifyComplete();
    }

    private Product product(String id) {
        return Product.builder()
                .id(id)
                .name("Product")
                .description("A product description")
                .price(BigDecimal.valueOf(10.0))
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
