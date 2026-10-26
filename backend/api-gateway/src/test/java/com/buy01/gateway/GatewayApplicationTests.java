package com.buy01.gateway;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class GatewayApplicationTests {

	@Test
	void canBeInstantiated() {
		new GatewayApplication();
	}

	@Test
	void mainDelegatesToSpringApplication() {
		String[] args = { "--spring.main.banner-mode=off" };

		try (MockedStatic<SpringApplication> springApplication = Mockito
				.mockStatic(SpringApplication.class)) {
			springApplication
					.when(() -> SpringApplication.run(GatewayApplication.class, args))
					.thenReturn(null);

			GatewayApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(GatewayApplication.class, args));
		}
	}

}
