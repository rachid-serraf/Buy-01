package com.buy01.media.infrastructure.web.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.media.application.OrphanedMediaCleanupJob;

@RestController 
@RequestMapping ("/internal/media")
@Profile ("dev") 
public class MediaCleanupTestController {

    private final OrphanedMediaCleanupJob job;

    public MediaCleanupTestController(OrphanedMediaCleanupJob job) {
        this.job = job;
    }

    @PostMapping ("/cleanup")
    public ResponseEntity<String> runCleanup() {
        job.cleanupOrphanedPendingMedia();
        return ResponseEntity.ok("Cleanup executed");
    }
}