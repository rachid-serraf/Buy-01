package com.buy01.user.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.buy01.user.domain.exception.InvalidCredentialsException;
import com.buy01.user.domain.exception.UserAlreadyExistsException;
import com.buy01.user.domain.exception.UserNotFoundException;

import io.jsonwebtoken.JwtException;

class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = org.mockito.Mockito.mock(WebRequest.class);

    public GlobalExceptionHandlerUnitTest() {
        org.mockito.Mockito.when(request.getDescription(false)).thenReturn("uri=/test");
    }

    @Test
    void handleUserAlreadyExists() {
        ProblemDetail pd = handler.handleUserAlreadyExists(new UserAlreadyExistsException("exists"), request);
        assertEquals(HttpStatus.CONFLICT, HttpStatus.valueOf(pd.getStatus()));
        assertNotNull(pd.getTitle());
    }

    @Test
    void handleInvalidCredentials() {
        ProblemDetail pd = handler.handleInvalidCredentials(new InvalidCredentialsException("bad"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleUserNotFound() {
        ProblemDetail pd = handler.handleUserNotFound(new UserNotFoundException("not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleAccessDenied() {
        ProblemDetail pd = handler.handleAccessDenied(new AccessDeniedException("denied"), request);
        assertEquals(HttpStatus.FORBIDDEN, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleBadCredentials() {
        ProblemDetail pd = handler.handleBadCredentials(new BadCredentialsException("bad"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleUserDisabled() {
        ProblemDetail pd = handler.handleUserDisabled(new DisabledException("disabled"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleAuthenticationException() {
        ProblemDetail pd = handler.handleAuthenticationException(new AuthenticationException("auth") {
        }, request);
        assertEquals(HttpStatus.UNAUTHORIZED, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleValidationErrors() throws Exception {
        // Create a mock binding result
        org.springframework.validation.BeanPropertyBindingResult bindingResult = 
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new org.springframework.validation.FieldError("object", "field", "msg"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ProblemDetail pd = handler.handleValidationErrors(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleTypeMismatchUuid() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", java.util.UUID.class, "id", null, new IllegalArgumentException("bad"));
        ProblemDetail pd = handler.handleTypeMismatch(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleTypeMismatchOther() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "age", null, new IllegalArgumentException("bad"));
        ProblemDetail pd = handler.handleTypeMismatch(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleConstraintViolation() {
        ProblemDetail pd = handler.handleConstraintViolation(new jakarta.validation.ConstraintViolationException(new HashSet<>()), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleInvalidJson() {
        ProblemDetail pd = handler.handleInvalidJson(new org.springframework.http.converter.HttpMessageNotReadableException("bad", (Throwable) null), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleIllegalArgument() {
        ProblemDetail pd = handler.handleIllegalArgument(new IllegalArgumentException("bad"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleMissingRequestParameter() {
        ProblemDetail pd = handler.handleMissingRequestParameter(new MissingServletRequestParameterException("email", "String"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleMissingPathVariable() throws Exception {
        ProblemDetail pd = handler.handleMissingPathVariable(
                new MissingPathVariableException("id", org.springframework.core.MethodParameter.forExecutable(
                        this.getClass().getDeclaredMethod("dummy"), -1)), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    void dummy() {
    }

    @Test
    void handleUnsupportedMediaType() {
        ProblemDetail pd = handler.handleUnsupportedMediaType(
                new org.springframework.web.HttpMediaTypeNotSupportedException("bad"), request);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleMediaTypeNotAcceptable() {
        ProblemDetail pd = handler.handleMediaTypeNotAcceptable(
                new org.springframework.web.HttpMediaTypeNotAcceptableException(Collections.emptyList()), request);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleMethodNotSupported() {
        ProblemDetail pd = handler.handleMethodNotSupported(
                new org.springframework.web.HttpRequestMethodNotSupportedException("PUT", Collections.singleton("GET")), request);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleEndpointNotFound() {
        ProblemDetail pd = handler.handleEndpointNotFound(
                new org.springframework.web.servlet.NoHandlerFoundException("GET", "/x", new HttpHeaders()), request);
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleResourceNotFound() {
        ProblemDetail pd = handler.handleResourceNotFound(
                new org.springframework.web.servlet.resource.NoResourceFoundException(HttpMethod.GET, "/x"), request);
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleDataIntegrityViolation() {
        ProblemDetail pd = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("dup"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleJwtException() {
        ProblemDetail pd = handler.handleJwtException(new JwtException("bad"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleMultipartException() {
        ProblemDetail pd = handler.handleMultipartException(new org.springframework.web.multipart.MultipartException("bad"), request);
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleFileSizeExceeded() {
        ProblemDetail pd = handler.handleFileSizeExceeded(new org.springframework.web.multipart.MaxUploadSizeExceededException(1000), request);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleFileNotFound() {
        ProblemDetail pd = handler.handleFileNotFound(new NoSuchFileException("x"), request);
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf(pd.getStatus()));
    }

    @Test
    void handleGenericException() {
        ProblemDetail pd = handler.handleGenericException(new RuntimeException("boom"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.valueOf(pd.getStatus()));
    }
}
