package com.edgareldy.springboottutorial.validation.password;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PasswordsMatchValidator}, driven through the full
 * Bean Validation stack ({@link Validator}) against {@link RegisterRequest}
 * which carries the {@link PasswordsMatch} annotation. Using the real
 * validator stack rather than calling the validator directly exercises the
 * constraint violation property node wiring that maps the error to the
 * {@code confirmPassword} field.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
class PasswordsMatchValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void _01_ShouldPassValidation_WhenPasswordsMatch() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada", "ada@example.com", "Password1@", "Password1@");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        boolean hasPasswordsMismatch = violations.stream()
                .anyMatch(v -> "confirmPassword".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("Passwords do not match"));
        assertThat(hasPasswordsMismatch).isFalse();
    }

    @Test
    void _02_ShouldFailOnConfirmPassword_WhenPasswordsDontMatch() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada", "ada@example.com", "Password1@", "Different1@");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        boolean hasPasswordsMismatch = violations.stream()
                .anyMatch(v -> "confirmPassword".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("Passwords do not match"));
        assertThat(hasPasswordsMismatch).isTrue();
    }
}
