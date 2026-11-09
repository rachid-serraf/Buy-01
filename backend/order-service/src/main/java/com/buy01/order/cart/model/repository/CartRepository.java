package com.buy01.order.cart.model.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.buy01.order.cart.model.entity.Cart;

@Repository 
public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserId(UUID userId);
}