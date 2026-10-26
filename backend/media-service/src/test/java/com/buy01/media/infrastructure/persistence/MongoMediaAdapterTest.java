package com.buy01.media.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buy01.media.domain.model.FileStatus;
import com.buy01.media.domain.model.Media;

@ExtendWith(MockitoExtension.class)
class MongoMediaAdapterTest {

    @Mock
    private SpringDataProductRepository mediaRepository;

    @InjectMocks
    private MongoMediaAdapter adapter;

    @Test
    void save_mapsAndDelegatesToRepository() {
        Media media = Media.builder()
                .imagePath("/images/a.png")
                .productId("product-1")
                .userId("user-1")
                .status(FileStatus.PENDING)
                .build();

        MediaDocument savedDocument = new MediaDocument();
        savedDocument.setId("media-1");
        savedDocument.setImagePath("/images/a.png");
        savedDocument.setProductId("product-1");
        savedDocument.setUserId("user-1");
        savedDocument.setStatus(FileStatus.PENDING);
        savedDocument.setCreatedAt(Instant.now());

        when(mediaRepository.save(any(MediaDocument.class))).thenReturn(savedDocument);

        Media result = adapter.save(media);

        assertThat(result.getId()).isEqualTo("media-1");
        assertThat(result.getImagePath()).isEqualTo("/images/a.png");
        assertThat(result.getProductId()).isEqualTo("product-1");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getStatus()).isEqualTo(FileStatus.PENDING);
        verify(mediaRepository).save(any(MediaDocument.class));
    }

    @Test
    void findById_returnsMappedMedia_whenPresent() {
        MediaDocument document = new MediaDocument();
        document.setId("media-2");
        document.setImagePath("/images/b.png");
        document.setProductId("product-2");
        document.setUserId("user-2");
        document.setStatus(FileStatus.CONFIRMED);
        document.setCreatedAt(Instant.now());

        when(mediaRepository.findById("media-2")).thenReturn(Optional.of(document));

        Optional<Media> result = adapter.findById("media-2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("media-2");
        assertThat(result.get().getImagePath()).isEqualTo("/images/b.png");
        assertThat(result.get().getProductId()).isEqualTo("product-2");
        assertThat(result.get().getUserId()).isEqualTo("user-2");
        assertThat(result.get().getStatus()).isEqualTo(FileStatus.CONFIRMED);
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        when(mediaRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<Media> result = adapter.findById("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndCreatedAtBefore_mapsList() {
        Instant threshold = Instant.now();
        MediaDocument document = new MediaDocument();
        document.setId("media-3");
        document.setImagePath("/images/c.png");
        document.setProductId("product-3");
        document.setUserId("user-3");
        document.setStatus(FileStatus.PENDING);
        document.setCreatedAt(threshold.minusSeconds(600));

        when(mediaRepository.findByStatusAndCreatedAtBefore(FileStatus.PENDING, threshold))
                .thenReturn(List.of(document));

        List<Media> result = adapter.findByStatusAndCreatedAtBefore(FileStatus.PENDING, threshold);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("media-3");
        assertThat(result.get(0).getStatus()).isEqualTo(FileStatus.PENDING);
    }

    @Test
    void deleteById_delegatesToRepository() {
        adapter.deleteById("media-4");
        verify(mediaRepository).deleteById("media-4");
    }

    @Test
    void findByProductId_mapsList() {
        MediaDocument document = new MediaDocument();
        document.setId("media-5");
        document.setImagePath("/images/d.png");
        document.setProductId("product-4");
        document.setUserId("user-4");
        document.setStatus(FileStatus.CONFIRMED);
        document.setCreatedAt(Instant.now());

        when(mediaRepository.findByProductId("product-4")).thenReturn(List.of(document));

        List<Media> result = adapter.findByProductId("product-4");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo("product-4");
    }

    @Test
    void findByProductIdIn_mapsList() {
        MediaDocument document = new MediaDocument();
        document.setId("media-6");
        document.setImagePath("/images/e.png");
        document.setProductId("product-5");
        document.setUserId("user-5");
        document.setStatus(FileStatus.PENDING);
        document.setCreatedAt(Instant.now());

        when(mediaRepository.findByProductIdIn(Set.of("product-5"))).thenReturn(List.of(document));

        List<Media> result = adapter.findByProductIdIn(Set.of("product-5"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("media-6");
    }
}
