package com.buy01.user.application.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class AvatarServiceImplTest {

    private AvatarServiceImpl avatarService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        avatarService = new AvatarServiceImpl();
        ReflectionTestUtils.setField(avatarService, "uploadDir", tempDir.toString());
        avatarService.init();
    }

    @Test
    void saveAvatarReturnsNullForEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", new byte[0]);
        assertNull(avatarService.saveAvatar(file));
        assertNull(avatarService.saveAvatar(null));
    }

    @Test
    void saveAvatarThrowsForLargeFile() {
        byte[] largeContent = new byte[3 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", largeContent);
        assertThrows(IllegalArgumentException.class, () -> avatarService.saveAvatar(file));
    }

    @Test
    void saveAvatarThrowsForInvalidType() {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.txt", "text/plain", "hello".getBytes());
        assertThrows(IllegalArgumentException.class, () -> avatarService.saveAvatar(file));
    }

    @Test
    void saveAndDeleteAvatarWork() throws IOException {
        byte[] pngBytes = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10}; // PNG header
        MockMultipartFile file = new MockMultipartFile("avatar", "test.png", "image/png", pngBytes);
        String url = avatarService.saveAvatar(file);
        assertNotNull(url);
        assertTrue(url.startsWith("/avatars/"));

        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path savedFile = tempDir.resolve(filename);
        assertTrue(Files.exists(savedFile));

        avatarService.deleteAvatar(url);
        assertFalse(Files.exists(savedFile));

        // Delete non-existent should not throw
        avatarService.deleteAvatar("/avatars/nonexistent.png");
        avatarService.deleteAvatar(null);
        avatarService.deleteAvatar("");
    }
}
