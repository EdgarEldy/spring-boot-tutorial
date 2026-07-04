package edgareldy.springboottutorial;

import org.springframework.boot.SpringApplication;

public class TestSpringBootTutorialApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringBootTutorialApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
