package com.oshimarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-only-secret-not-used-outside-of-tests-32bytes")
class OshiMarketApplicationTests {

	@Test
	void contextLoads() {
	}

}
