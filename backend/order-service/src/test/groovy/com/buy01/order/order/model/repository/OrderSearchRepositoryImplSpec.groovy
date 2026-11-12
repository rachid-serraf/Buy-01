package com.buy01.order.order.model.repository

import java.time.Instant

import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query

import com.buy01.order.common.Status
import com.buy01.order.order.model.entity.Order

import spock.lang.Specification

class OrderSearchRepositoryImplSpec extends Specification {

    private MongoTemplate mongoTemplate
    private OrderSearchRepositoryImpl repository
    private PageRequest pageable
    private Query captured

    def setup() {
        mongoTemplate = Mock()
        repository = new OrderSearchRepositoryImpl(mongoTemplate)
        pageable = PageRequest.of(0, 20)
        captured = null
    }

    private static String json(Query query) {
        query.getQueryObject().toString()
    }

    def "returns all orders when no filter is given"() {
        when:
        def page = repository.search(null, null, null, null, null, null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        page.totalElements == 25
        page.content*.id == ["order-1"]
        page.totalPages == 2
        captured.getQueryObject().isEmpty()
    }

    def "filters by buyer"() {
        when:
        repository.search("buyer-1", null, null, null, null, null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("buyerId")
        json(captured).contains("buyer-1")
    }

    def "filters by seller on the order or its items"() {
        when:
        repository.search(null, "seller-1", null, null, null, null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("\$or")
        json(captured).contains("sellerId")
        json(captured).contains("items.sellerId")
    }

    def "filters by status"() {
        when:
        repository.search(null, null, null, Status.SHIPPED, null, null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("status")
        json(captured).contains("SHIPPED")
    }

    def "filters by a closed date range"() {
        when:
        repository.search(
                null, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("createdAt")
        json(captured).contains("\$gte")
        json(captured).contains("\$lt")
    }

    def "filters by a start date only"() {
        when:
        repository.search(
                null, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("createdAt")
        json(captured).contains("\$gte")
        !json(captured).contains("\$lt")
    }

    def "filters by an end date only"() {
        when:
        repository.search(
                null, null, null, null,
                null, Instant.parse("2026-02-01T00:00:00Z"), pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("createdAt")
        json(captured).contains("\$lt")
        !json(captured).contains("\$gte")
    }

    def "searches by object id and item name"() {
        when:
        repository.search(null, null, "507f1f77bcf86cd799439011", null, null, null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("\$or")
        json(captured).contains("_id")
        json(captured).contains("items.name")
        json(captured).contains("507f1f77bcf86cd799439011")
    }

    def "searches by text term and trims the input"() {
        when:
        repository.search(null, null, "  phone  ", null, null, null, pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("\$or")
        json(captured).contains("phone")
        !json(captured).contains("  phone  ")
    }

    def "combines every filter"() {
        when:
        repository.search(
                "buyer-1", "seller-1", "phone", Status.PENDING,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                pageable)

        then:
        1 * mongoTemplate.count(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            25L
        }
        1 * mongoTemplate.find(_ as Query, _ as Class) >> { Query query, Object type ->
            captured = query
            [Order.builder().id("order-1").build()]
        }
        json(captured).contains("buyerId")
        json(captured).contains("sellerId")
        json(captured).contains("status")
        json(captured).contains("createdAt")
        json(captured).contains("items.name")
    }
}
