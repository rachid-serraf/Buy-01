package com.buy01.product.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void builderExposesAllFields() {
        Instant created = Instant.parse("2026-03-25T10:00:00Z");
        Instant updated = Instant.parse("2026-03-25T11:00:00Z");
        Product product = Product.builder()
                .id("prod-1")
                .name("Product")
                .description("A product description")
                .price(10.0)
                .quantity(2)
                .userId("seller-1")
                .imagesIds(List.of("img-1"))
                .category(Category.ELECTRONICS)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        assertThat(product.getId()).isEqualTo("prod-1");
        assertThat(product.getName()).isEqualTo("Product");
        assertThat(product.getDescription()).isEqualTo("A product description");
        assertThat(product.getPrice()).isEqualTo(10.0);
        assertThat(product.getQuantity()).isEqualTo(2);
        assertThat(product.getUserId()).isEqualTo("seller-1");
        assertThat(product.getImagesIds()).containsExactly("img-1");
        assertThat(product.getCategory()).isEqualTo(Category.ELECTRONICS);
        assertThat(product.getCreatedAt()).isEqualTo(created);
        assertThat(product.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toBuilderCopiesExistingProductValues() {
        Instant created = Instant.parse("2026-03-25T10:00:00Z");
        Product original = Product.builder()
                .id("prod-1")
                .name("Product")
                .description("A product description")
                .price(10.0)
                .quantity(2)
                .userId("seller-1")
                .imagesIds(List.of("img-1"))
                .category(Category.ELECTRONICS)
                .createdAt(created)
                .updatedAt(created)
                .build();

        Product copy = original.toBuilder()
                .name("Updated")
                .price(20.0)
                .category(Category.CLOTHING)
                .build();

        assertThat(copy.getId()).isEqualTo("prod-1");
        assertThat(copy.getName()).isEqualTo("Updated");
        assertThat(copy.getDescription()).isEqualTo("A product description");
        assertThat(copy.getPrice()).isEqualTo(20.0);
        assertThat(copy.getQuantity()).isEqualTo(2);
        assertThat(copy.getUserId()).isEqualTo("seller-1");
        assertThat(copy.getCategory()).isEqualTo(Category.CLOTHING);
        assertThat(copy.getCreatedAt()).isEqualTo(created);
        assertThat(copy.getUpdatedAt()).isEqualTo(created);
    }

    @Test
    void validateRejectsMissingOrBlankName() {
        assertThatThrownBy(() -> Product.builder()
                .name(null)
                .price(10.0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name is required");

        assertThatThrownBy(() -> Product.builder()
                .name("  ")
                .price(10.0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name is required");
    }

    @Test
    void validateRejectsNonPositivePrice() {
        assertThatThrownBy(() -> Product.builder()
                .name("Product")
                .price(0.0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price must be positive.");

        assertThatThrownBy(() -> Product.builder()
                .name("Product")
                .price(-1.0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price must be positive.");
    }

    @Test
    void validateAcceptsValidProduct() {
        Product product = Product.builder()
                .name("Product")
                .price(1.0)
                .category(Category.OTHER)
                .build();

        product.validate();

        assertThat(product.getName()).isEqualTo("Product");
        assertThat(product.getCategory()).isEqualTo(Category.OTHER);
    }
}
