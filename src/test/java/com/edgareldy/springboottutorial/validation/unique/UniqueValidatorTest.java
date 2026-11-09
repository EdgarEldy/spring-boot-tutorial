package com.edgareldy.springboottutorial.validation.unique;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.entity.user.Role;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for {@link UniqueValidator}, driven through the full
 * Bean Validation stack against a real PostgreSQL instance provided by
 * Testcontainers. Validates that {@link Unique} rejects values that already
 * exist in the database and accepts values that do not.
 * <p>
 * {@code @DirtiesContext} is not used because each test is isolated: the
 * "duplicate" tests create their own entity and check a different field or
 * value, so there is no cross-test contamination of the database state.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UniqueValidatorTest.TestcontainersConfig.class)
class UniqueValidatorTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));
        }
    }

    @Autowired
    private Validator validator;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void _01_ShouldPassValidation_WhenUsernameIsUnique() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "uniqueuser", "uniqueuser@example.com", "Password1@", "Password1@");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        boolean hasUsernameViolation = violations.stream()
                .anyMatch(v -> "username".equals(v.getPropertyPath().toString()));
        assertThat(hasUsernameViolation).isFalse();
    }

    @Test
    void _02_ShouldFailOnUsername_WhenUsernameAlreadyExists() {
        appUserRepository.save(AppUser.builder()
                .firstName("Existing").lastName("User")
                .username("taken_user").email("taken_user@example.com")
                .password("hashed").role(Role.USER).build());

        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "taken_user", "new@example.com", "Password1@", "Password1@");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        boolean hasUsernameViolation = violations.stream()
                .anyMatch(v -> "username".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("already in use"));
        assertThat(hasUsernameViolation).isTrue();
    }

    @Test
    void _03_ShouldFailOnEmail_WhenEmailAlreadyExistsCaseInsensitive() {
        appUserRepository.save(AppUser.builder()
                .firstName("Existing").lastName("User")
                .username("other_user").email("TAKEN@EXAMPLE.COM")
                .password("hashed").role(Role.USER).build());

        // Different case but same email - @Unique(caseSensitive=false) should catch it
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "new_user", "taken@example.com", "Password1@", "Password1@");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> "email".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("already in use"));
        assertThat(hasEmailViolation).isTrue();
    }
}
