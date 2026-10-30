package com.edgareldy.springboottutorial;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringBootTutorialApplicationTests {

	@Test
	void contextLoads() {
	}

}
