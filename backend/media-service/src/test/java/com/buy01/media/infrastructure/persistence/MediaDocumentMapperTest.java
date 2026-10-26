package com.buy01.media.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.buy01.media.domain.model.FileStatus;
import com.buy01.media.domain.model.Media;

class MediaDocumentMapperTest {

    @Test
    void privateConstructor_isInvocable() throws Exception {
        Constructor<MediaDocumentMapper> constructor = MediaDocumentMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void toDocument_mapsAllFields_whenIdAndCreatedAtPresent() {
        Instant now = Instant.now();
        Media media = Media.builder()
                .id("media-1")
                .imagePath("/images/test.png")
                .createdAt(now)
                .productId("product-1")
                .userId("user-1")
                .status(FileStatus.PENDING)
                .build();

        MediaDocument document = MediaDocumentMapper.toDocument(media);

        assertThat(document.getId()).isEqualTo("media-1");
        assertThat(document.getImagePath()).isEqualTo("/images/test.png");
        assertThat(document.getCreatedAt()).isEqualTo(now);
        assertThat(document.getProductId()).isEqualTo("product-1");
        assertThat(document.getUserId()).isEqualTo("user-1");
        assertThat(document.getStatus()).isEqualTo(FileStatus.PENDING);
    }

    @Test
    void toDocument_setsCreatedAtToNow_whenCreatedAtNull() {
        Media media = Media.builder()
                .imagePath("/images/test2.png")
                .createdAt(null)
                .productId("product-2")
                .userId("user-2")
                .status(FileStatus.CONFIRMED)
                .build();

        MediaDocument document = MediaDocumentMapper.toDocument(media);

        assertThat(document.getId()).isNull();
        assertThat(document.getCreatedAt()).isNotNull();
        assertThat(document.getImagePath()).isEqualTo("/images/test2.png");
        assertThat(document.getProductId()).isEqualTo("product-2");
        assertThat(document.getUserId()).isEqualTo("user-2");
        assertThat(document.getStatus()).isEqualTo(FileStatus.CONFIRMED);
    }

    @Test
    void toDomain_mapsAllFields() {
        Instant now = Instant.now();
        MediaDocument document = new MediaDocument();
        document.setId("media-3");
        document.setImagePath("/images/test3.png");
        document.setCreatedAt(now);
        document.setProductId("product-3");
        document.setUserId("user-3");
        document.setStatus(FileStatus.PENDING);

        Media media = MediaDocumentMapper.toDomain(document);

        assertThat(media.getId()).isEqualTo("media-3");
        assertThat(media.getImagePath()).isEqualTo("/images/test3.png");
        assertThat(media.getCreatedAt()).isEqualTo(now);
        assertThat(media.getProductId()).isEqualTo("product-3");
        assertThat(media.getUserId()).isEqualTo("user-3");
        assertThat(media.getStatus()).isEqualTo(FileStatus.PENDING);
    }
}
