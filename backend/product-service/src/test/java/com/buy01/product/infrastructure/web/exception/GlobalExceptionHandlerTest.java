package com.buy01.product.infrastructure.web.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SignatureException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebInputException;

import com.buy01.product.infrastructure.web.exception.Errors.MediaServiceException;

import io.jsonwebtoken.ExpiredJwtException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesSignatureException() {
        assertProblem(new SignatureException("bad signature"), HttpStatus.UNAUTHORIZED, "Invalid JWT Signature");
    }

    @Test
    void handlesExpiredJwtException() {
        assertProblem(new ExpiredJwtException(null, null, "expired"), HttpStatus.UNAUTHORIZED, "JWT Token Expired");
    }

    @Test
    void handlesAuthorizationDeniedException() {
        assertProblem(new AuthorizationDeniedException("denied"), HttpStatus.FORBIDDEN, "Access Denied");
    }

    @Test
    void handlesNoResourceFoundException() {
        assertProblem(new NoResourceFoundException("/missing"), HttpStatus.BAD_REQUEST, "Missing Argument");
    }

    @Test
    void handlesHttpMessageNotReadableException() {
        assertProblem(new HttpMessageNotReadableException("bad json"), HttpStatus.BAD_REQUEST, "JSON Not Valid");
    }

    @Test
    void handlesIllegalArgumentException() {
        assertProblem(new IllegalArgumentException("bad argument"), HttpStatus.BAD_REQUEST, "Bad Argument");
    }

    @Test
    void handlesNotFoundException() {
        assertProblem(new NotFoundException(), HttpStatus.NOT_FOUND, "Validation Error");
    }

    @Test
    void handlesUsernameNotFoundException() {
        assertProblem(new UsernameNotFoundException("missing user"), HttpStatus.NOT_FOUND, "User Not Found");
    }

    @Test
    void handlesMethodNotAllowedException() {
        assertProblem(new MethodNotAllowedException("POST", List.of(org.springframework.http.HttpMethod.GET)),
                HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
    }

    @Test
    void handlesServerWebInputException() {
        assertProblem(new ServerWebInputException("no body"), HttpStatus.BAD_REQUEST, "No request body");
    }

    @Test
    void handlesWebExchangeBindExceptionWithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError(
                "request", "name", "", false, new String[] { "NotBlank" }, new Object[] {}, "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        WebExchangeBindException exception = new WebExchangeBindException(mock(MethodParameter.class), bindingResult);

        ProblemDetail problem = handler.handle(exception).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
        assertThat(problem.getDetail()).isEqualTo("name: must not be blank");
    }

    @Test
    void handlesWebExchangeBindExceptionWithoutDefaultMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError(
                "request", "price", 0, true, new String[] { "Min" }, new Object[] { 1 }, null);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        WebExchangeBindException exception = new WebExchangeBindException(mock(MethodParameter.class), bindingResult);

        ProblemDetail problem = handler.handle(exception).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).startsWith("price: ").contains("Field error in object");
    }

    @Test
    void handlesUnknownExceptionWithDefaultTemplate() {
        ProblemDetail problem = handler.handle(new RuntimeException("unexpected")).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("unexpected");
    }

    @Test
    void handlesMediaServiceClientError() {
        ProblemDetail problem = handler.handleMediaServiceException(
                new MediaServiceException(404, "not found", "body")).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Media Service Client Error");
        assertThat(problem.getDetail()).isEqualTo("not found → body");
        assertThat(problem.getProperties())
                .containsEntry("externalService", "media")
                .containsEntry("externalStatus", 404)
                .containsEntry("externalDetail", "body");
    }

    @Test
    void handlesMediaServiceServerError() {
        ProblemDetail problem = handler.handleMediaServiceException(
                new MediaServiceException(503, "unavailable", "busy")).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Media Service Server Error");
    }

    @Test
    void handlesMediaServiceNonErrorStatus() {
        ProblemDetail problem = handler.handleMediaServiceException(
                new MediaServiceException(200, "ok", null)).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(problem.getTitle()).isEqualTo("Media Service Unavailable");
        assertThat(problem.getDetail()).isEqualTo("ok");
        assertThat(problem.getProperties()).doesNotContainKey("externalDetail");
    }

    @Test
    void handlesMediaServiceInvalidStatusAsInternalServerError() {
        ProblemDetail problem = handler.handleMediaServiceException(
                new MediaServiceException(999, "invalid", "body")).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Media Service Server Error");
    }

    @Test
    void handlesMediaServiceZeroStatusAsInternalServerError() {
        ProblemDetail problem = handler.handleMediaServiceException(
                new MediaServiceException(0, "network", null)).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Media Service Server Error");
    }

    @Test
    void handlesMediaServiceBlankResponseBody() {
        ProblemDetail problem = handler.handleMediaServiceException(
                new MediaServiceException(400, "bad", "  ")).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo("bad");
    }

    private void assertProblem(Exception exception, HttpStatus status, String title) {
        ProblemDetail problem = handler.handle(exception).block();

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
    }
}
