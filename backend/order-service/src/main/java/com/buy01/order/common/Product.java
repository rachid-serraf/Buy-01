package com.buy01.order.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@Data
@NoArgsConstructor 
public class Product {
    private String id;
    private String name;
    private double price;
    // Getters and Setters
}
