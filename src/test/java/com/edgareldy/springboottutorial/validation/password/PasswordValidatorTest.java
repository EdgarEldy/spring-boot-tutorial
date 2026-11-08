package com.edgareldy.springboottutorial.validation.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PasswordValidator}, verifying that the regex
 * enforces all four password complexity rules independently and in
 * combination: at least 8 non-whitespace characters, one uppercase letter,
 * one digit, and one special character from the allowed set.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void _01_ShouldAcceptNull_BecauseNotBlankHandlesThat() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void _02_ShouldAccept_WhenAllRulesMet() {
        assertThat(validator.isValid("Password1@", null)).isTrue();
    }

    @Test
    void _03_ShouldReject_WhenTooShort() {
        // 7 chars, has uppercase, digit, special - still too short
        assertThat(validator.isValid("Pass1@a", null)).isFalse();
    }

    @Test
    void _04_ShouldReject_WhenNoUppercase() {
        assertThat(validator.isValid("password1@long", null)).isFalse();
    }

    @Test
    void _05_ShouldReject_WhenNoDigit() {
        assertThat(validator.isValid("Password@@long", null)).isFalse();
    }

    @Test
    void _06_ShouldReject_WhenNoSpecialChar() {
        assertThat(validator.isValid("Password1long", null)).isFalse();
    }

    @Test
    void _07_ShouldReject_WhenContainsWhitespace() {
        assertThat(validator.isValid("Pass word1@", null)).isFalse();
    }

    @Test
    void _08_ShouldAccept_WhenAllAllowedSpecialCharsUsed() {
        for (char special : new char[]{'@', '#', '$', '%', '^', '&', '+', '=', '!'}) {
            String password = "Passw0rd" + special;
            assertThat(validator.isValid(password, null))
                    .as("Expected valid for special char '%s'", special)
                    .isTrue();
        }
    }
}
