package com.buy01.product.infrastructure.web.exception;

import java.util.List;

public class Errors {
    public static class MediaServiceException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        public MediaServiceException(int statusCode, String message, String responseBody) {
            super(message != null ? message : "Media service error (" + statusCode + ")");
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }

    public static class InvalidSearchException extends RuntimeException {
        private final List<String> details;

        public InvalidSearchException(String message, List<String> details) {
            super(message);
            this.details = details;
        }

        public String getCode() {
            return "INVALID_SEARCH_PARAMS";
        }

        public List<String> getDetails() {
            return details;
        }
    }

}
