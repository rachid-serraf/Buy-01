package com.buy01.order.order.model.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddress {

    @NotBlank(message = "fullName is required")
    @Size(max = 120, message = "fullName must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "phone is required")
    @Size(max = 30, message = "phone must be at most 30 characters")
    private String phone;

    @NotBlank(message = "line1 is required")
    @Size(max = 200, message = "line1 must be at most 200 characters")
    private String line1;

    @Size(max = 200, message = "line2 must be at most 200 characters")
    private String line2;

    @NotBlank(message = "city is required")
    @Size(max = 100, message = "city must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "state must be at most 100 characters")
    private String state;

    @NotBlank(message = "postalCode is required")
    @Size(max = 20, message = "postalCode must be at most 20 characters")
    private String postalCode;

    @NotBlank(message = "country is required")
    @Size(max = 100, message = "country must be at most 100 characters")
    private String country;
}
