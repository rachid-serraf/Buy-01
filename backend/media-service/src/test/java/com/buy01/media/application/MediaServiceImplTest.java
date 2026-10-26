package com.buy01.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.media.domain.model.FileStatus;
import com.buy01.media.domain.model.Media;
import com.buy01.media.domain.ports.outbound.MediaRepositoryPort;
import com.buy01.media.infrastructure.web.exception.Errors.Faileduploadedfile;
import com.buy01.media.infrastructure.web.exception.Errors.NotFound;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    @Mock
    private MediaRepositoryPort repository;

    @TempDir
    Path tempDir;

    private MediaServiceImpl service;
    private Path uploadedFile;

    @BeforeEach
    void setUp() throws IOException {
        service = new MediaServiceImpl(repository);
    }

    @AfterEach
    void cleanUpUploadedFile() throws IOException {
        if (uploadedFile != null) {
            Files.deleteIfExists(uploadedFile);
        }
    }

    @Test
    void getImageFileRejectsNullId() {
        assertStatus(() -> service.getImageFile(null), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getImageFileRejectsBlankId() {
        assertStatus(() -> service.getImageFile("   "), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getImageFileRejectsUnknownMedia() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertStatus(() -> service.getImageFile("missing"), HttpStatus.NOT_FOUND);
    }

    @Test
    void getImageFileRejectsMediaWithNullPath() {
        Media media = Media.builder().id("img-1").imagePath(null).build();
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        assertStatus(() -> service.getImageFile("img-1"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getImageFileRejectsMediaWithBlankPath() {
        Media media = Media.builder().id("img-1").imagePath(" ").build();
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        assertStatus(() -> service.getImageFile("img-1"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getImageFileRejectsUnreadableFile() throws IOException {
        Path image = Files.write(tempDir.resolve("unreadable.png"), PNG_BYTES);
        Assumptions.assumeTrue(
                Files.getFileStore(image).supportsFileAttributeView("posix"),
                "POSIX file permissions are required for this test");
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(image);
        Files.setPosixFilePermissions(image, Set.of());

        try {
            Assumptions.assumeFalse(Files.isReadable(image), "cannot simulate an unreadable file");

            Media media = Media.builder().id("img-1").imagePath(image.toString()).build();
            when(repository.findById("img-1")).thenReturn(Optional.of(media));

            assertStatus(() -> service.getImageFile("img-1"), HttpStatus.FORBIDDEN);
        } finally {
            Files.setPosixFilePermissions(image, originalPermissions);
        }
    }

    @Test
    void getImageFileReturnsExistingReadableResource() throws IOException {
        Path image = Files.write(tempDir.resolve("image.png"), PNG_BYTES);
        Media media = Media.builder().id("img-1").imagePath(image.toString()).build();
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        Resource resource = service.getImageFile("img-1");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void getImageFileRejectsMissingFileOnDisk() {
        Media media = Media.builder()
                .id("img-1")
                .imagePath(tempDir.resolve("missing.png").toString())
                .build();
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        assertStatus(() -> service.getImageFile("img-1"), HttpStatus.NOT_FOUND);
    }

    @Test
    void detectContentTypeUsesTika() {
        MediaType mediaType = service.detectContentType(new ByteArrayResource(PNG_BYTES));

        assertThat(mediaType).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void detectContentTypeFallsBackToJpegOnReadFailure() throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenThrow(new IOException("cannot read"));

        assertThat(service.detectContentType(resource)).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadImage(file, "user-1"))
                .isInstanceOf(Faileduploadedfile.class)
                .hasMessage("File is empty");
    }

    @Test
    void uploadImageRejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("files", "big.png", "image/png",
                new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.uploadImage(file, "user-1"))
                .isInstanceOf(Faileduploadedfile.class)
                .hasMessageContaining("exceeds 2MB");
    }

    @Test
    void uploadImageRejectsInvalidMimeType() {
        MockMultipartFile file = new MockMultipartFile("files", "notes.txt", "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.uploadImage(file, "user-1"))
                .isInstanceOf(Faileduploadedfile.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void uploadImageRejectsFileThatCannotBeReadForDetection() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getBytes()).thenThrow(new IOException("read error"));

        assertThatThrownBy(() -> service.uploadImage(file, "user-1"))
                .isInstanceOf(Faileduploadedfile.class)
                .hasMessageContaining("Could not read file for type detection");
    }

    @Test
    void uploadImageStoresFileAndSavesPendingMedia() {
        MockMultipartFile file = new MockMultipartFile("files", "photo.png", "image/png", PNG_BYTES);
        when(repository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media saved = service.uploadImage(file, "user-1");

        uploadedFile = Paths.get(saved.getImagePath());
        assertThat(saved.getImagePath()).startsWith("uploads/").endsWith(".png");
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getStatus()).isEqualTo(FileStatus.PENDING);
        assertThat(Files.exists(uploadedFile)).isTrue();
    }

    @Test
    void uploadImageStoresFileWithoutExtensionWhenNameHasNoDot() {
        MockMultipartFile file = new MockMultipartFile("files", "photo", "image/png", PNG_BYTES);
        when(repository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media saved = service.uploadImage(file, "user-1");

        uploadedFile = Paths.get(saved.getImagePath());
        assertThat(saved.getImagePath()).doesNotContain(".");
    }

    @Test
    void uploadImageDeletesStoredFileWhenSaveFails() {
        MockMultipartFile file = new MockMultipartFile("files", "photo.png", "image/png", PNG_BYTES);
        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        when(repository.save(any(Media.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.uploadImage(file, "user-1"))
                .isInstanceOf(Faileduploadedfile.class)
                .hasMessageContaining("Failed to save media metadata");

        verify(repository).save(mediaCaptor.capture());
        assertThat(Files.exists(Paths.get(mediaCaptor.getValue().getImagePath()))).isFalse();
    }

    @Test
    void getProdutImagesReturnsMediaIds() {
        when(repository.findByProductId("prod-1")).thenReturn(List.of(
                media("img-1", "p1", "user-1"),
                media("img-2", "p1", "user-1")));

        assertThat(service.getProdutImages("prod-1")).containsExactly("img-1", "img-2");
    }

    @Test
    void findImageUrlsByProductIdsMapsFirstImageAndKeepsMissingProducts() {
        when(repository.findByProductIdIn(any())).thenReturn(List.of(
                media("img-1", "prod-1", "user-1"),
                media("img-2", "prod-1", "user-1"),
                media("img-3", "prod-2", "user-1")));

        Map<String, String> result = service.findImageUrlsByProductIds(
                List.of("prod-1", "prod-2", "prod-3"));

        assertThat(result)
                .containsEntry("prod-1", "img-1")
                .containsEntry("prod-2", "img-3")
                .containsEntry("prod-3", null);
    }

    @Test
    void deleteByIdRejectsNullId() {
        assertThatThrownBy(() -> service.deleteById(null, "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteByIdRejectsUnknownMedia() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById("missing", "user-1"))
                .isInstanceOf(NotFound.class)
                .hasMessageContaining("missing");
    }

    @Test
    void deleteByIdRejectsAnotherUsersMedia() {
        Media media = media("img-1", "prod-1", "owner");
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> service.deleteById("img-1", "intruder"))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteByIdIgnoresFailureToDeleteStoredFile() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("media-directory"));
        Files.write(directory.resolve("child.txt"), PNG_BYTES);
        Media media = Media.builder()
                .id("img-1")
                .imagePath(directory.toString())
                .userId("owner")
                .build();
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        assertThatCode(() -> service.deleteById("img-1", "owner")).doesNotThrowAnyException();

        verify(repository).deleteById("img-1");
        assertThat(Files.exists(directory)).isTrue();
    }

    @Test
    void deleteByIdRemovesMetadataAndStoredFile() throws IOException {
        Path image = Files.write(tempDir.resolve("delete-me.png"), PNG_BYTES);
        Media media = Media.builder()
                .id("img-1")
                .imagePath(image.toString())
                .userId("owner")
                .build();
        when(repository.findById("img-1")).thenReturn(Optional.of(media));

        assertThatCode(() -> service.deleteById("img-1", "owner")).doesNotThrowAnyException();

        verify(repository).deleteById("img-1");
        assertThat(Files.exists(image)).isFalse();
    }

    private Media media(String id, String productId, String userId) {
        return Media.builder()
                .id(id)
                .imagePath("/uploads/" + id + ".png")
                .productId(productId)
                .userId(userId)
                .status(FileStatus.LINKED)
                .build();
    }

    private void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(expected));
    }
}
