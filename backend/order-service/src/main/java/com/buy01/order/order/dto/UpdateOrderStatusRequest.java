package com.buy01.order.order.dto;

import com.buy01.order.common.Status;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "status is required")
        Status status) {
}
