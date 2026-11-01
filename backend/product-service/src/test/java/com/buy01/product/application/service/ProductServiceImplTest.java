package com.buy01.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.buy01.product.domain.model.Category;
import com.buy01.product.domain.model.Product;
import com.buy01.product.domain.ports.outbound.ProductRepositoryPort;
import com.buy01.product.infrastructure.web.dto.ProductSearchRequest;
import com.buy01.product.infrastructure.web.dto.ProductResponse;
import com.buy01.product.infrastructure.web.exception.Errors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private ImageService imageService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, imageService);
    }

    @Test
    void createProductSetsTimestampsImagesAndPublishesEvent() {
        Product product = product("prod-1", "seller-1");
        List<String> images = List.of("img-1", "img-2");

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return Mono.just(saved.toBuilder().id("prod-1").build());
        });

        StepVerifier.create(productService.createProduct(product, images))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo("prod-1");
                    assertThat(saved.getImagesIds()).containsExactly("img-1", "img-2");
                    assertThat(saved.getCategory()).isEqualTo(Category.ELECTRONICS);
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(imageService).sendKafkaImageLinked(any(Product.class), eq(images));
    }

    @Test
    void createProductPropagatesRepositoryFailure() {
        Product product = product("prod-1", "seller-1");
        when(productRepository.save(any(Product.class)))
                .thenReturn(Mono.error(new IllegalStateException("database down")));

        StepVerifier.create(productService.createProduct(product, List.of("img-1")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("database down"))
                .verify();

        verify(imageService, never()).sendKafkaImageLinked(any(Product.class), any());
    }

    @Test
    void updateProductUpdatesFieldsAndPublishesEvent() {
        Product existing = product("prod-1", "seller-1");
        Product update = Product.builder()
                .name("Updated")
                .description("Updated description")
                .price(BigDecimal.valueOf(20.0))
                .quantity(4)
                .imagesIds(List.of("img-new"))
                .category(Category.CLOTHING)
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Mono.just(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return Mono.just(saved.toBuilder().id("prod-1").build());
        });

        StepVerifier.create(productService.updateProduct(update, "prod-1", "seller-1"))
                .assertNext(saved -> {
                    assertThat(saved.getName()).isEqualTo("Updated");
                    assertThat(saved.getDescription()).isEqualTo("Updated description");
                    assertThat(saved.getPrice()).isEqualTo(BigDecimal.valueOf(20.0));
                    assertThat(saved.getQuantity()).isEqualTo(4);
                    assertThat(saved.getCategory()).isEqualTo(Category.CLOTHING);
                    assertThat(saved.getCreatedAt()).isEqualTo(existing.getCreatedAt());
                })
                .verifyComplete();

        verify(imageService).sendKafkaImageLinked(any(Product.class), eq(List.of("img-new")));
    }

    @Test
    void updateProductKeepsExistingCategoryWhenUpdateCategoryIsNull() {
        Product existing = product("prod-1", "seller-1");
        Product update = Product.builder()
                .name("Updated")
                .description("Updated description")
                .price(BigDecimal.valueOf(20.0))
                .quantity(4)
                .imagesIds(List.of("img-new"))
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Mono.just(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(productService.updateProduct(update, "prod-1", "seller-1"))
                .assertNext(saved -> assertThat(saved.getCategory()).isEqualTo(Category.ELECTRONICS))
                .verifyComplete();
    }

    @Test
    void updateProductFailsWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(productService.updateProduct(product("p", "seller-1"), "missing", "seller-1"))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Product not found"))
                .verify();

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductFailsWhenUserDoesNotOwnProduct() {
        when(productRepository.findById("prod-1")).thenReturn(Mono.just(product("prod-1", "seller-1")));

        StepVerifier.create(productService.updateProduct(product("p", "other"), "prod-1", "other"))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Not your product"))
                .verify();

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductWithImagesCombinesProductAndImages() {
        Product product = product("prod-1", "seller-1");
        when(productRepository.findById("prod-1")).thenReturn(Mono.just(product));
        when(imageService.getProductImages("prod-1")).thenReturn(Mono.just(List.of("img-1")));

        StepVerifier.create(productService.getProductWithImages("prod-1"))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("prod-1");
                    assertThat(response.getImagesIds()).containsExactly("img-1");
                    assertThat(response.getCategory()).isEqualTo(Category.ELECTRONICS);
                })
                .verifyComplete();
    }

    @Test
    void getProductWithImagesReturnsEmptyWhenProductIsMissing() {
        when(productRepository.findById("missing")).thenReturn(Mono.empty());
        when(imageService.getProductImages("missing")).thenReturn(Mono.just(List.of()));

        StepVerifier.create(productService.getProductWithImages("missing"))
                .verifyComplete();
    }

    @Test
    void getProductsListMapsProductsAndImagesWithDefaultForMissingImage() {
        Product first = product("prod-1", "seller-1");
        Product second = product("prod-2", "seller-2");
        Instant before = Instant.parse("2026-03-25T12:00:00Z");

        when(productRepository.findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(before))
                .thenReturn(Flux.just(first, second));
        when(imageService.getImagesBatch(List.of("prod-1", "prod-2")))
                .thenReturn(Mono.just(Map.of("prod-1", "url-1")));

        StepVerifier.create(productService.getProductsList(before))
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo("prod-1");
                    assertThat(item.getImage()).isEqualTo("url-1");
                    assertThat(item.getCategory()).isEqualTo(Category.ELECTRONICS);
                })
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo("prod-2");
                    assertThat(item.getImage()).isEmpty();
                    assertThat(item.getCategory()).isEqualTo(Category.ELECTRONICS);
                })
                .verifyComplete();
    }

    @Test
    void getProductsListReturnsEmptyWhenRepositoryIsEmpty() {
        Instant before = Instant.parse("2026-03-25T12:00:00Z");
        when(productRepository.findTop10ByCreatedAtBeforeOrderByCreatedAtDesc(before))
                .thenReturn(Flux.empty());

        StepVerifier.create(productService.getProductsList(before))
                .verifyComplete();

        verify(imageService, never()).getImagesBatch(any());
    }

    @Test
    void getUserProductsListMapsProductsAndImages() {
        Product product = product("prod-1", "seller-1");
        Instant before = Instant.parse("2026-03-25T12:00:00Z");

        when(productRepository.findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc("seller-1", before))
                .thenReturn(Flux.just(product));
        when(imageService.getImagesBatch(List.of("prod-1")))
                .thenReturn(Mono.just(Map.of("prod-1", "url-1")));

        StepVerifier.create(productService.getUserProductsList("seller-1", before))
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo("prod-1");
                    assertThat(item.getImage()).isEqualTo("url-1");
                    assertThat(item.getCategory()).isEqualTo(Category.ELECTRONICS);
                })
                .verifyComplete();
    }

    @Test
    void getUserProductsListReturnsEmptyWhenRepositoryIsEmpty() {
        Instant before = Instant.parse("2026-03-25T12:00:00Z");
        when(productRepository.findTop10ByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc("seller-1", before))
                .thenReturn(Flux.empty());

        StepVerifier.create(productService.getUserProductsList("seller-1", before))
                .verifyComplete();

        verify(imageService, never()).getImagesBatch(any());
    }

    @Test
    void deleteProductDeletesOwnedProductAndPublishesEvent() {
        when(productRepository.findById("prod-1")).thenReturn(Mono.just(product("prod-1", "seller-1")));
        when(productRepository.deleteById("prod-1")).thenReturn(Mono.empty());

        StepVerifier.create(productService.deleteProduct("prod-1", "seller-1"))
                .verifyComplete();

        verify(productRepository).deleteById("prod-1");
        verify(imageService).sendKafkaProductDeleted("prod-1");
    }

    @Test
    void deleteProductFailsWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(productService.deleteProduct("missing", "seller-1"))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Product not found"))
                .verify();

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void deleteProductFailsWhenUserDoesNotOwnProduct() {
        when(productRepository.findById("prod-1")).thenReturn(Mono.just(product("prod-1", "seller-1")));

        StepVerifier.create(productService.deleteProduct("prod-1", "other"))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Not your product"))
                .verify();

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void deleteProductPropagatesDeleteFailure() {
        when(productRepository.findById("prod-1")).thenReturn(Mono.just(product("prod-1", "seller-1")));
        when(productRepository.deleteById("prod-1"))
                .thenReturn(Mono.error(new IllegalStateException("delete failed")));

        StepVerifier.create(productService.deleteProduct("prod-1", "seller-1"))
                .expectError(IllegalStateException.class)
                .verify();

        verify(imageService, never()).sendKafkaProductDeleted(any());
    }

    @Test
    void searchAppliesFiltersPaginationAndSortAndMapsThePageResponse() {
        ProductSearchRequest request = new ProductSearchRequest(
                "phone", "ELECTRONICS", 100.0, 500.0, "price_asc", 0, 12);
        Product phone = product("phone-1", "seller-1").toBuilder()
                .name("Phone")
                .price(BigDecimal.valueOf(199.99))
                .build();

        when(productRepository.search(eq(request), any(Pageable.class)))
                .thenReturn(Mono.just(new PageImpl<>(List.of(phone), Pageable.ofSize(12), 25)));

        StepVerifier.create(productService.search(request))
                .assertNext(response -> {
                    assertThat(response.page()).isZero();
                    assertThat(response.size()).isEqualTo(12);
                    assertThat(response.totalElements()).isEqualTo(25);
                    assertThat(response.totalPages()).isEqualTo(3);
                    assertThat(response.content()).singleElement().satisfies(result -> {
                        assertThat(result.id()).isEqualTo("phone-1");
                        assertThat(result.name()).isEqualTo("Phone");
                        assertThat(result.price()).isEqualByComparingTo("199.99");
                        assertThat(result.category()).isEqualTo(Category.ELECTRONICS);
                    });
                })
                .verifyComplete();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).search(eq(request), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(12);
        assertThat(pageable.getValue().getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void searchRejectsInvalidFiltersWithoutCallingTheRepository() {
        ProductSearchRequest request = new ProductSearchRequest(
                "phone", null, -1.0, -2.0, "unsupported", -1, 51);

        StepVerifier.create(productService.search(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(Errors.InvalidSearchException.class);
                    Errors.InvalidSearchException invalid = (Errors.InvalidSearchException) error;
                    assertThat(invalid.getCode()).isEqualTo("INVALID_SEARCH_PARAMS");
                    assertThat(invalid.getDetails()).contains(
                            "minPrice must be >= 0",
                            "maxPrice must be >= 0",
                            "page must be >= 0",
                            "size must be between 1 and 50",
                            "sort must be one of: newest, oldest, price_asc, price_desc, name");
                })
                .verify();

        verify(productRepository, never()).search(any(), any());
    }

    private Product product(String id, String userId) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .description("A product description")
                .price(BigDecimal.valueOf(10.0))
                .quantity(2)
                .userId(userId)
                .imagesIds(List.of("img-1"))
                .category(Category.ELECTRONICS)
                .createdAt(Instant.parse("2026-03-25T10:00:00Z"))
                .updatedAt(Instant.parse("2026-03-25T10:00:00Z"))
                .build();
    }
}
