package com.buy01.order.order.model.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.buy01.order.common.Status;
import com.buy01.order.order.model.entity.Order;

public interface OrderSearchRepository {

    Page<Order> search(
            String buyerId,
            String sellerId,
            String search,
            Status status,
            Instant from,
            Instant toExclusive,
            Pageable pageable);
}
