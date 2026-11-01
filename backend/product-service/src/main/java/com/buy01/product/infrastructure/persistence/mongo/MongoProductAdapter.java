package com.buy01.product.infrastructure.persistence.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.buy01.product.infrastructure.web.dto.ProductSearchRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.buy01.product.domain.model.Product;
import com.buy01.product.domain.ports.outbound.ProductRepositoryPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class MongoProductAdapter implements ProductRepositoryPort {

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
    public Mono<Page<Product>> search(ProductSearchRequest r, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();

        if (r.q() != null && !r.q().isBlank()) {
            String regex = Pattern.quote(r.q().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i")));
        }

        if (r.category() != null && !r.category().isBlank()) {
            filters.add(Criteria.where("category").is(r.category().trim().toLowerCase()));
        }

        if (r.minPrice() != null || r.maxPrice() != null) {
            Criteria price = Criteria.where("price");
            if (r.minPrice() != null) price = price.gte(r.minPrice());
            if (r.maxPrice() != null) price = price.lte(r.maxPrice());
            filters.add(price);
        }

        Query countQuery = new Query();
        if (!filters.isEmpty()) {
            countQuery.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }
        Query findQuery = Query.of(countQuery).with(pageable);

        Mono<List<Product>> content = template.find(findQuery, Product.class).collectList();
        Mono<Long> total = template.count(countQuery, Product.class);

        return Mono.zip(content, total)
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

}
