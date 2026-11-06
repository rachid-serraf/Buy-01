package com.buy01.order.cart.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@Document (collection = "carts")
public class Cart {

    @Id 
    private String id;

    @Indexed(unique = true)
    private UUID userId;            

    private List<CartItem> items = new ArrayList<>();

    @CreatedDate 
    private Instant createdAt;

    @LastModifiedDate 
    private Instant updatedAt;
}
