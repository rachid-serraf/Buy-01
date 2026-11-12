package com.buy01.order.order.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.buy01.order.common.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String checkoutId;

    @Indexed
    private String buyerId;

    @Indexed
    private String sellerId;

    @Indexed
    private Status status;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal total;

    private DeliveryAddress address;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;
}
