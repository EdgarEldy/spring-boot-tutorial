package com.edgareldy.springboottutorial.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * {@link ConstraintValidator} implementation for {@link ValidPassword}.
 * Accepts {@code null} (let {@code @NotBlank} handle that) and validates
 * non-null values against a single regex that checks all complexity rules
 * in one pass.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])" +       // at least one uppercase letter
            "(?=.*\\d)" +           // at least one digit
            "(?=.*[@#$%^&+=!])" +  // at least one special character
            "\\S{8,}$"             // no whitespace, minimum 8 characters
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return PASSWORD_PATTERN.matcher(value).matches();
    }
}
