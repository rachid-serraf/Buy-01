package com.buy01.product.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

import com.buy01.product.domain.model.Category;
import jakarta.validation.constraints.*;

public record ProductCreateRequest(
        @NotBlank(message = "Product name is required") @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters") String name,

        @NotBlank(message = "Product description is required") @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters") String description,

        @Min(value = 1, message = "Price must be at least 1.0") @Max(value = 10000, message = "Price cannot exceed 10,000.0") BigDecimal price,

        @Min(value = 0, message = "Quantity cannot be negative") @Max(value = 1000, message = "Quantity cannot exceed 1,000 units") int quantity,

        @NotEmpty(message = "At least one media ID is required") @Size(min = 1, max = 10, message = "You can upload between 1 and 10 images") List<String> imagesIds,

        @NotNull(message="Category is required") Category category
) {
}
