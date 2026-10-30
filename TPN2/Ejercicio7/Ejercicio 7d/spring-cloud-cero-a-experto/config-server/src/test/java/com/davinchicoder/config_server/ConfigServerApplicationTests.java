package com.davinchicoder.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles({"local", "native"})
@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class ConfigServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
