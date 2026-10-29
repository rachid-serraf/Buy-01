package com.buy01.product.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.buy01.product.domain.model.Category;

public class ProductList {
    private String id;
    private String name;
    private BigDecimal price;
    private String image;
    private Category category;
    private Instant createdAt;

    public ProductList(String id, String name, BigDecimal price, Instant createdAt, String image) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.createdAt = createdAt;
    }

    public ProductList(String id, String name, BigDecimal price, Instant createdAt, String image, Category category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
        this.createdAt = createdAt;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
