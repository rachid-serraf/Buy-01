package com.buy01.order.order.dto;

import java.time.LocalDate;

import com.buy01.order.common.Status;

public record OrderQuery(
        String search,
        Status status,
        LocalDate from,
        LocalDate to,
        int page,
        int size) {
}
