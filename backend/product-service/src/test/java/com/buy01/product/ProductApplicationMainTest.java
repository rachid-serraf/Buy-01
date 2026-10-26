package com.buy01.product;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ProductApplicationMainTest {

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = { "--spring.profiles.active=test" };

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ProductApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ProductApplication.class, args));
        }
    }
}
