package com.buy01.product.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.buy01.product.application.events.UserDeletedEvent;

class MessagingEventTest {

    @Test
    void imagesLinkedEventExposesFields() {
        ImagesLinkedEvent event = new ImagesLinkedEvent("prod-1", List.of("img-1"));

        assertThat(event.productId()).isEqualTo("prod-1");
        assertThat(event.mediaIds()).containsExactly("img-1");
    }

    @Test
    void userDeletedEventExposesFields() {
        UserDeletedEvent event = new UserDeletedEvent("user-1");

        assertThat(event.userId()).isEqualTo("user-1");
    }
}
