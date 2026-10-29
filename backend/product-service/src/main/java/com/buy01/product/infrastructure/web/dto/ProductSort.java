package com.buy01.product.infrastructure.web.dto;


import org.springframework.data.domain.Sort;

public enum ProductSort {
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    OLDEST(Sort.by(Sort.Direction.ASC, "createdAt")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "price")),
    NAME(Sort.by(Sort.Direction.ASC, "name"));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    /** Whitelist parse: null/blank -> NEWEST, unknown -> null. */
    public static ProductSort parse(String value) {
        if (value == null || value.isBlank()) return NEWEST;
        for (ProductSort s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) return s;
        }
        return null;
    }
}
