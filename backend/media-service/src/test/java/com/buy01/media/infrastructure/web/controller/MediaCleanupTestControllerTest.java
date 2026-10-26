package com.buy01.media.infrastructure.web.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.buy01.media.application.OrphanedMediaCleanupJob;

@WebMvcTest(MediaCleanupTestController.class)
@ActiveProfiles("dev")
class MediaCleanupTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrphanedMediaCleanupJob job;

    @Test
    void runCleanup_triggersJobAndReturnsOk() throws Exception {
        mockMvc.perform(post("/internal/media/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cleanup executed"));

        verify(job).cleanupOrphanedPendingMedia();
    }
}
