package com.buy01.order.order.model.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.buy01.order.common.Status;
import com.buy01.order.order.model.entity.Order;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderSearchRepositoryImpl implements OrderSearchRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Order> search(
            String buyerId,
            String sellerId,
            String search,
            Status status,
            Instant from,
            Instant toExclusive,
            Pageable pageable) {

        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(buyerId)) {
            criteria.add(Criteria.where("buyerId").is(buyerId));
        }

        if (StringUtils.hasText(sellerId)) {
            criteria.add(new Criteria().orOperator(
                    Criteria.where("sellerId").is(sellerId),
                    Criteria.where("items.sellerId").is(sellerId)));
        }

        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }

        if (from != null || toExclusive != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (from != null && toExclusive != null) {
                criteria.add(createdAt.gte(from).lt(toExclusive));
            } else if (from != null) {
                criteria.add(createdAt.gte(from));
            } else {
                criteria.add(createdAt.lt(toExclusive));
            }
        }

        if (StringUtils.hasText(search)) {
            String term = search.trim();
            String quoted = Pattern.quote(term);

            List<Criteria> searchCriteria = new ArrayList<>();
            searchCriteria.add(Criteria.where("_id").is(term));
            if (ObjectId.isValid(term)) {
                searchCriteria.add(Criteria.where("_id").is(new ObjectId(term)));
            }
            searchCriteria.add(Criteria.where("_id").regex(quoted, "i"));
            searchCriteria.add(Criteria.where("items.name").regex(quoted, "i"));

            criteria.add(new Criteria().orOperator(searchCriteria.toArray(Criteria[]::new)));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        long total = mongoTemplate.count(query, Order.class);
        query.with(pageable);
        List<Order> content = mongoTemplate.find(query, Order.class);

        return new PageImpl<>(content, pageable, total);
    }
}
