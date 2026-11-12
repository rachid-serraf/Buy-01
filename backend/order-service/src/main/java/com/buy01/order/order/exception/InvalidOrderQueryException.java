package com.buy01.order.order.exception;

import java.util.List;

public class InvalidOrderQueryException extends RuntimeException {

    private final List<String> details;

    public InvalidOrderQueryException(List<String> details) {
        super("Invalid order query");
        this.details = List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
