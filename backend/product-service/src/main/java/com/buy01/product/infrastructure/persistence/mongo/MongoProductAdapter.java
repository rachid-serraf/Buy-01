package com.buy01.product.infrastructure.persistence.mongo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.buy01.product.infrastructure.web.dto.ProductList;
import com.buy01.product.infrastructure.web.dto.ProductSearchRequest;
import lombok.AllArgsConstructor;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.buy01.product.domain.model.Product;
import com.buy01.product.domain.ports.outbound.ProductRepositoryPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class MongoProductAdapter implements ProductRepositoryPort {

    private static final BigDecimal MAX_PRICE = new BigDecimal("1E+30");

    private final SpringDataProductRepository springRepo;
    private final ProductDocumentMapper mapper;
    private final ReactiveMongoTemplate template;

    @Override
    public Mono<Product> save(Product product) {
        var savedProduct = springRepo.save(mapper.toDocument(product));
        return savedProduct.map(mapper::toDomain);
    }

    @Override
    public Mono<Product> findById(String id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Product> findByUserId(String userId) {
        return springRepo.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return springRepo.deleteById(id);
    }

    @Override
    public Flux<Product> findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(Instant lastProduct) {
        var productList = springRepo.findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(lastProduct);
        return productList.map(mapper::toDomain);
    }

    @Override
    public Flux<Product> findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(String userId, Instant beforeTime) {
        var productList = springRepo.findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(userId, beforeTime);
        return productList.map(mapper::toDomain);
    }

    @Override
    public Mono<Page<ProductList>> search(ProductSearchRequest r, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();

        if (r.q() != null && !r.q().isBlank()) {
            String regex = Pattern.quote(r.q().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i")));
        }

        if (r.category() != null && !r.category().isBlank()) {
            filters.add(Criteria.where("category").is(r.category().trim().toUpperCase(java.util.Locale.ROOT)));
        }

        BigDecimal minPrice = r.minPrice() != null ? BigDecimal.valueOf(r.minPrice()) : BigDecimal.ZERO;
        BigDecimal maxPrice = r.maxPrice() != null ? BigDecimal.valueOf(r.maxPrice()) : MAX_PRICE;
        filters.add(Criteria.where("price")
                .gte(new Decimal128(minPrice))
                .lte(new Decimal128(maxPrice)));

        Criteria match = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        AggregationOperation normalizePrice = Aggregation.addFields()
                .addField("price")
                .withValue(ConvertOperators.Convert.convertValueOf("price")
                        .to("decimal")
                        .onErrorReturn(0)
                        .onNullReturn(0))
                .build();

        Aggregation contentAggregation = Aggregation.newAggregation(
                normalizePrice,
                Aggregation.match(match),
                Aggregation.sort(pageable.getSort()),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize()),
                Aggregation.project()
                        .andInclude("_id", "name", "price", "category")
                        .and("created_at").as("createdAt"));

        Aggregation countAggregation = Aggregation.newAggregation(
                normalizePrice,
                Aggregation.match(match),
                Aggregation.count().as("total"));

        Mono<List<ProductList>> content = template
                .aggregate(contentAggregation, ProductDocument.class, ProductList.class)
                .collectList();

        Mono<Long> total = template
                .aggregate(countAggregation, ProductDocument.class, org.bson.Document.class)
                .next()
                .map(document -> ((Number) document.get("total")).longValue())
                .defaultIfEmpty(0L);

        return Mono.zip(content, total)
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

}
