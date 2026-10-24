package com.buy01.media.infrastructure.web.exception;

public class Errors {

    private Errors(){}

    public static class Faileduploadedfile extends RuntimeException {
        public Faileduploadedfile(String message) {
            super(message);
        }
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String message) {
            super(message);
        }
    }

    public static class PublicKeyInitializationException extends RuntimeException {

        public PublicKeyInitializationException(String message) {
            super(message);
        }

        public PublicKeyInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
