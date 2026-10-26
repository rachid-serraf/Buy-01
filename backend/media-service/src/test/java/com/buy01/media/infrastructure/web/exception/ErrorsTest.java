package com.buy01.media.infrastructure.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorsTest {

    @Test
    void failedUploadedFileKeepsMessage() {
        Errors.Faileduploadedfile error = new Errors.Faileduploadedfile("upload failed");

        assertThat(error).hasMessage("upload failed");
    }

    @Test
    void notFoundKeepsMessage() {
        Errors.NotFound error = new Errors.NotFound("not found");

        assertThat(error).hasMessage("not found");
    }

    @Test
    void publicKeyInitializationExceptionKeepsMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        Errors.PublicKeyInitializationException withMessage =
                new Errors.PublicKeyInitializationException("invalid key");
        Errors.PublicKeyInitializationException withCause =
                new Errors.PublicKeyInitializationException("invalid key", cause);

        assertThat(withMessage).hasMessage("invalid key");
        assertThat(withCause).hasMessage("invalid key").hasCause(cause);
    }
}
