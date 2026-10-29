package com.buy01.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.buy01.product.domain.model.Product;
import com.buy01.product.infrastructure.messaging.ImagesLinkedEvent;
import com.buy01.product.infrastructure.web.dto.ProductDeletedEvent;
import com.buy01.product.infrastructure.web.exception.Errors.MediaServiceException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private KafkaTemplate<String, ImagesLinkedEvent> imageLinkedTemplate;

    @Mock
    private KafkaTemplate<String, ProductDeletedEvent> productDeletedTemplate;

    @Test
    void sendKafkaImageLinkedPublishesEventWhenImagesExist() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.OK, "")));

        service.sendKafkaImageLinked(product("prod-1"), List.of("img-1", "img-2"));

        verify(imageLinkedTemplate).send(
                "images-linked",
                "prod-1",
                new ImagesLinkedEvent("prod-1", List.of("img-1", "img-2")));
    }

    @Test
    void sendKafkaImageLinkedSkipsEventWhenThereAreNoImages() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.OK, "")));

        service.sendKafkaImageLinked(product("prod-1"), List.of());

        verifyNoInteractions(imageLinkedTemplate);
    }

    @Test
    void sendKafkaProductDeletedPublishesEvent() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.OK, "")));

        service.sendKafkaProductDeleted("prod-1");

        verify(productDeletedTemplate).send(
                "product-deleted",
                "prod-1",
                new ProductDeletedEvent("prod-1"));
    }

    @Test
    void getProductImagesReturnsEmptyForNullOrBlankId() {
        ImageService service = service(exchange -> {
            throw new AssertionError("Media service should not be called");
        });

        StepVerifier.create(service.getProductImages(null))
                .assertNext(images -> assertThat(images).isEmpty())
                .verifyComplete();
        StepVerifier.create(service.getProductImages("  "))
                .assertNext(images -> assertThat(images).isEmpty())
                .verifyComplete();
    }

    @Test
    void getProductImagesReturnsImages() {
        ImageService service = service(exchange -> {
            assertThat(exchange.url().getPath()).isEqualTo("/media/product/prod-1");
            return Mono.just(plainResponse(HttpStatus.OK, "url-1"));
        });

        StepVerifier.create(service.getProductImages("prod-1"))
                .assertNext(images -> assertThat(images).containsExactly("url-1"))
                .verifyComplete();
    }

    @Test
    void getProductImagesMapsClientError() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.NOT_FOUND, "missing")));

        StepVerifier.create(service.getProductImages("prod-1"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(MediaServiceException.class);
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isEqualTo(404);
                    assertThat(mediaError.getMessage()).contains("client error for product prod-1");
                    assertThat(mediaError.getResponseBody()).isEqualTo("missing");
                })
                .verify();
    }

    @Test
    void getProductImagesUsesDefaultBodyForEmptyClientError() {
        ImageService service = service(exchange -> Mono.just(emptyResponse(HttpStatus.BAD_REQUEST)));

        StepVerifier.create(service.getProductImages("prod-1"))
                .expectErrorSatisfies(error -> {
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isEqualTo(400);
                    assertThat(mediaError.getResponseBody()).isEqualTo("No error details");
                })
                .verify();
    }

    @Test
    void getProductImagesMapsServerError() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.BAD_GATEWAY, "boom")));

        StepVerifier.create(service.getProductImages("prod-1"))
                .expectErrorSatisfies(error -> {
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isEqualTo(502);
                    assertThat(mediaError.getMessage()).contains("server error for product prod-1");
                })
                .verify();
    }

    @Test
    void getProductImagesWrapsTransportErrors() {
        ImageService service = service(exchange -> Mono.error(new RuntimeException("connection refused")));

        StepVerifier.create(service.getProductImages("prod-1"))
                .expectErrorSatisfies(error -> {
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isZero();
                    assertThat(mediaError.getMessage())
                            .contains("Failed to reach media service for product prod-1")
                            .contains("connection refused");
                    assertThat(mediaError.getResponseBody()).isNull();
                })
                .verify();
    }

    @Test
    void getImagesBatchReturnsEmptyForNullOrEmptyIds() {
        ImageService service = service(exchange -> {
            throw new AssertionError("Media service should not be called");
        });

        StepVerifier.create(service.getImagesBatch(null))
                .assertNext(images -> assertThat(images).isEmpty())
                .verifyComplete();
        StepVerifier.create(service.getImagesBatch(List.of()))
                .assertNext(images -> assertThat(images).isEmpty())
                .verifyComplete();
    }

    @Test
    void getImagesBatchReturnsMap() {
        ImageService service = service(exchange -> {
            assertThat(exchange.url().getPath()).isEqualTo("/media/batch");
            return Mono.just(response(HttpStatus.OK, "{\"prod-1\":\"url-1\"}"));
        });

        StepVerifier.create(service.getImagesBatch(List.of("prod-1")))
                .assertNext(images -> assertThat(images)
                        .containsExactlyEntriesOf(Map.of("prod-1", "url-1")))
                .verifyComplete();
    }

    @Test
    void getImagesBatchMapsClientError() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.BAD_REQUEST, "bad request")));

        StepVerifier.create(service.getImagesBatch(List.of("prod-1")))
                .expectErrorSatisfies(error -> {
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isEqualTo(400);
                    assertThat(mediaError.getMessage()).isEqualTo("Media service client error");
                    assertThat(mediaError.getResponseBody()).isEqualTo("bad request");
                })
                .verify();
    }

    @Test
    void getImagesBatchUsesDefaultBodyForEmptyClientError() {
        ImageService service = service(exchange -> Mono.just(emptyResponse(HttpStatus.BAD_REQUEST)));

        StepVerifier.create(service.getImagesBatch(List.of("prod-1")))
                .expectErrorSatisfies(error -> assertThat(((MediaServiceException) error).getResponseBody())
                        .isEqualTo("No error body"))
                .verify();
    }

    @Test
    void getImagesBatchMapsServerError() {
        ImageService service = service(exchange -> Mono.just(response(HttpStatus.INTERNAL_SERVER_ERROR, "oops")));

        StepVerifier.create(service.getImagesBatch(List.of("prod-1")))
                .expectErrorSatisfies(error -> {
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isEqualTo(500);
                    assertThat(mediaError.getMessage()).isEqualTo("Media service server error");
                })
                .verify();
    }

    @Test
    void getImagesBatchUsesDefaultBodyForEmptyServerError() {
        ImageService service = service(exchange -> Mono.just(emptyResponse(HttpStatus.INTERNAL_SERVER_ERROR)));

        StepVerifier.create(service.getImagesBatch(List.of("prod-1")))
                .expectErrorSatisfies(error -> assertThat(((MediaServiceException) error).getResponseBody())
                        .isEqualTo("No error body"))
                .verify();
    }

    @Test
    void getImagesBatchWrapsTransportErrors() {
        ImageService service = service(exchange -> Mono.error(new RuntimeException("timeout")));

        StepVerifier.create(service.getImagesBatch(List.of("prod-1")))
                .expectErrorSatisfies(error -> {
                    MediaServiceException mediaError = (MediaServiceException) error;
                    assertThat(mediaError.getStatusCode()).isZero();
                    assertThat(mediaError.getMessage())
                            .contains("Failed to reach media service")
                            .contains("timeout");
                    assertThat(mediaError.getResponseBody()).isNull();
                })
                .verify();
    }

    private ImageService service(Function<ClientRequest, Mono<ClientResponse>> exchangeFunction) {
        WebClient client = WebClient.builder()
                .exchangeFunction(exchangeFunction::apply)
                .build();
        return new ImageService(client, imageLinkedTemplate, productDeletedTemplate);
    }

    private ClientResponse response(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private ClientResponse plainResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .body(body)
                .build();
    }

    private ClientResponse emptyResponse(HttpStatus status) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Product product(String id) {
        return Product.builder()
                .id(id)
                .name("Product")
                .description("A product description")
                .price(BigDecimal.valueOf(10.0))
                .quantity(1)
                .userId("seller-1")
                .createdAt(Instant.parse("2026-03-25T10:00:00Z"))
                .updatedAt(Instant.parse("2026-03-25T10:00:00Z"))
                .build();
    }
}
