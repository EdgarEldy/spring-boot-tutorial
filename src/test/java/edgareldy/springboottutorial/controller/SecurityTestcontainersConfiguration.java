package edgareldy.springboottutorial.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a Testcontainers Postgres instance for {@code @SpringBootTest}
 * classes in this package, mirroring the top-level
 * {@code TestcontainersConfiguration} and {@code repository}'s
 * {@code RepositoryTestcontainersConfiguration}. Kept local to
 * {@code controller/} rather than reusing either package-private class from
 * a different package.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@TestConfiguration(proxyBeanMethods = false)
class SecurityTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));
    }
}
