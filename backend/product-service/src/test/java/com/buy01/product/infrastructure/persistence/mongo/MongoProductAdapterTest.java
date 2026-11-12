package com.buy01.product.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;

import com.buy01.product.domain.model.Category;
import com.buy01.product.domain.model.Product;
import com.buy01.product.infrastructure.web.dto.ProductList;
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
        ProductList result = new ProductList(
                "prod-1",
                "Phone",
                BigDecimal.valueOf(199.99),
                Instant.parse("2026-03-25T10:00:00Z"),
                "",
                Category.ELECTRONICS);
        stubSearch(6L, result);

        StepVerifier.create(adapter.search(request, pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).containsExactly(result);
                    assertThat(page.getTotalElements()).isEqualTo(6);
                    assertThat(page.getNumber()).isEqualTo(1);
                    assertThat(page.getSize()).isEqualTo(5);
                })
                .verifyComplete();

        Aggregation aggregation = captureContentAggregation();

        assertThat(stages(aggregation))
                .containsExactly("$addFields", "$match", "$sort", "$skip", "$limit", "$project");

        Document price = priceClause(aggregation);
        assertPriceBound(price.get("$gte"), "100");
        assertPriceBound(price.get("$lte"), "500");

        assertThat(matchClause(aggregation).toJson())
                .contains("name", "description", "Phone.*", "category", "ELECTRONICS");

        assertThat(stage(aggregation, "$sort").get("$sort", Document.class))
                .containsEntry("price", 1);
        assertThat(stage(aggregation, "$skip").get("$skip")).isEqualTo(5L);
        assertThat(stage(aggregation, "$limit").get("$limit")).isEqualTo(5L);
        assertThat(stage(aggregation, "$project").get("$project", Document.class))
                .containsKeys("name", "price", "category", "createdAt");
    }

    @Test
    void searchNormalizesStringPricesBeforeFilteringAndSorting() {
        ProductSearchRequest request = new ProductSearchRequest(
                null, null, 100.0, 500.0, "price_asc", 0, 12);
        PageRequest pageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.ASC, "price"));
        stubSearch(0L);

        StepVerifier.create(adapter.search(request, pageable))
                .assertNext(page -> assertThat(page).isEmpty())
                .verifyComplete();

        Document addFields = stage(captureContentAggregation(), "$addFields")
                .get("$addFields", Document.class);
        Document convert = addFields.get("price", Document.class).get("$convert", Document.class);

        assertThat(convert)
                .containsEntry("input", "$price")
                .containsEntry("to", "decimal");
    }

    @Test
    void searchDefaultsPriceBoundsWhenBoundsAreMissing() {
        ProductSearchRequest request = new ProductSearchRequest(" ", " ", null, null, "newest", 0, 12);
        PageRequest pageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));
        stubSearch(0L);

        StepVerifier.create(adapter.search(request, pageable))
                .assertNext(page -> {
                    assertThat(page).isEmpty();
                    assertThat(page.getTotalElements()).isZero();
                })
                .verifyComplete();

        Document price = priceClause(captureContentAggregation());
        assertPriceBound(price.get("$gte"), "0");
        assertPriceBound(price.get("$lte"), "1E+30");
    }

    @Test
    void searchDefaultsMissingBoundaryToZeroOrMaximumPrice() {
        PageRequest pageable = PageRequest.of(0, 12);
        stubSearch(0L);

        StepVerifier.create(adapter.search(
                        new ProductSearchRequest(null, null, 100.0, null, "newest", 0, 12), pageable))
                .assertNext(page -> assertThat(page).isEmpty())
                .verifyComplete();
        StepVerifier.create(adapter.search(
                        new ProductSearchRequest(null, null, null, 500.0, "newest", 0, 12), pageable))
                .assertNext(page -> assertThat(page).isEmpty())
                .verifyComplete();

        ArgumentCaptor<Aggregation> aggregation = ArgumentCaptor.forClass(Aggregation.class);
        verify(template, times(2))
                .aggregate(aggregation.capture(), eq(ProductDocument.class), eq(ProductList.class));

        Document minOnly = priceClause(aggregation.getAllValues().get(0));
        assertPriceBound(minOnly.get("$gte"), "100");
        assertPriceBound(minOnly.get("$lte"), "1E+30");

        Document maxOnly = priceClause(aggregation.getAllValues().get(1));
        assertPriceBound(maxOnly.get("$gte"), "0");
        assertPriceBound(maxOnly.get("$lte"), "500");
    }

    private void assertPriceBound(Object actual, String expected) {
        assertThat(((Decimal128) actual).bigDecimalValue()).isEqualByComparingTo(expected);
    }

    private Aggregation captureContentAggregation() {
        ArgumentCaptor<Aggregation> aggregation = ArgumentCaptor.forClass(Aggregation.class);
        verify(template).aggregate(aggregation.capture(), eq(ProductDocument.class), eq(ProductList.class));
        return aggregation.getValue();
    }

    private List<String> stages(Aggregation aggregation) {
        return aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT).stream()
                .map(document -> document.keySet().iterator().next())
                .toList();
    }

    private Document stage(Aggregation aggregation, String name) {
        return aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT).stream()
                .filter(document -> document.containsKey(name))
                .findFirst()
                .orElseThrow();
    }

    private Document matchClause(Aggregation aggregation) {
        return stage(aggregation, "$match").get("$match", Document.class);
    }

    private Document priceClause(Aggregation aggregation) {
        List<Document> clauses = matchClause(aggregation).getList("$and", Document.class);
        return clauses.get(clauses.size() - 1).get("price", Document.class);
    }

    private void stubSearch(long total, ProductList... results) {
        when(template.aggregate(any(Aggregation.class), eq(ProductDocument.class), eq(ProductList.class)))
                .thenReturn(Flux.just(results));
        when(template.aggregate(any(Aggregation.class), eq(ProductDocument.class), eq(Document.class)))
                .thenReturn(Flux.just(new Document("total", total)));
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
