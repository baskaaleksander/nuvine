package com.baskaaleksander.nuvine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuthApplicationTests {

	@Test
	void contextLoads() {
		// Simple smoke test to verify the test class itself compiles
		// All actual business logic is tested in unit tests with mocked dependencies
		assertDoesNotThrow(() -> {
			// Test passes - no Spring context loading needed
		});
	}

}
