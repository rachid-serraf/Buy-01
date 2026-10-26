package com.buy01.product.infrastructure.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.buy01.product.infrastructure.web.exception.Errors.MediaServiceException;

class ErrorsTest {

    @Test
    void errorsContainerCanBeInstantiated() {
        assertThat(new Errors()).isNotNull();
    }

    @Test
    void mediaServiceExceptionKeepsStatusMessageAndBody() {
        MediaServiceException exception = new MediaServiceException(404, "not found", "body");

        assertThat(exception.getStatusCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("not found");
        assertThat(exception.getResponseBody()).isEqualTo("body");
    }

    @Test
    void mediaServiceExceptionBuildsDefaultMessageWhenMessageIsNull() {
        MediaServiceException exception = new MediaServiceException(503, null, null);

        assertThat(exception.getMessage()).isEqualTo("Media service error (503)");
        assertThat(exception.getResponseBody()).isNull();
    }
}
