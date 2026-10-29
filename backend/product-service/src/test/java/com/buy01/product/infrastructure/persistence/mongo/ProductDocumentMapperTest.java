package com.buy01.product.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.buy01.product.domain.model.Category;
import com.buy01.product.domain.model.Product;

class ProductDocumentMapperTest {

    private final ProductDocumentMapper mapper = new ProductDocumentMapper();

    @Test
    void toDocumentCopiesAllFields() {
        Instant created = Instant.parse("2026-03-25T10:00:00Z");
        Instant updated = Instant.parse("2026-03-25T11:00:00Z");
        Product product = Product.builder()
                .id("prod-1")
                .name("Product")
                .description("A product description")
                .price(BigDecimal.valueOf(12.5))
                .quantity(3)
                .userId("seller-1")
                .imagesIds(List.of("img-1"))
                .category(Category.ELECTRONICS)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        ProductDocument document = mapper.toDocument(product);

        assertThat(document.getId()).isEqualTo("prod-1");
        assertThat(document.getName()).isEqualTo("Product");
        assertThat(document.getDescription()).isEqualTo("A product description");
        assertThat(document.getPrice()).isEqualTo(BigDecimal.valueOf(12.5));
        assertThat(document.getQuantity()).isEqualTo(3);
        assertThat(document.getUserId()).isEqualTo("seller-1");
        assertThat(document.getCategory()).isEqualTo("ELECTRONICS");
        assertThat(document.getCreatedAt()).isEqualTo(created);
        assertThat(document.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toDomainCopiesAllFields() {
        Instant created = Instant.parse("2026-03-25T10:00:00Z");
        Instant updated = Instant.parse("2026-03-25T11:00:00Z");
        ProductDocument document = new ProductDocument();
        document.setId("prod-1");
        document.setName("Product");
        document.setDescription("A product description");
        document.setPrice(BigDecimal.valueOf(12.5));
        document.setQuantity(3);
        document.setUserId("seller-1");
        document.setCategory("CLOTHING");
        document.setCreatedAt(created);
        document.setUpdatedAt(updated);

        Product product = mapper.toDomain(document);

        assertThat(product.getId()).isEqualTo("prod-1");
        assertThat(product.getName()).isEqualTo("Product");
        assertThat(product.getDescription()).isEqualTo("A product description");
        assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(12.5));
        assertThat(product.getQuantity()).isEqualTo(3);
        assertThat(product.getUserId()).isEqualTo("seller-1");
        assertThat(product.getImagesIds()).isNull();
        assertThat(product.getCategory()).isEqualTo(Category.CLOTHING);
        assertThat(product.getCreatedAt()).isEqualTo(created);
        assertThat(product.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toDomainAllowsLegacyDocumentsWithoutCategory() {
        ProductDocument document = new ProductDocument();
        document.setId("prod-2");
        document.setName("Uncategorized");
        document.setDescription("A product without a category");
        document.setPrice(BigDecimal.ONE);
        document.setQuantity(1);
        document.setUserId("seller-1");

        Product mapped = mapper.toDomain(document);
        assertThat(mapped.getCategory()).isNull();
    }

    @Test
    void toDocumentAllowsProductsWithoutCategory() {
        Product product = mock(Product.class);
        when(product.getCategory()).thenReturn(null);

        ProductDocument document = mapper.toDocument(product);

        assertThat(document.getCategory()).isNull();
    }
}
