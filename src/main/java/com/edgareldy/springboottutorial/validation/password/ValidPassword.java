package com.edgareldy.springboottutorial.validation.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Bean Validation constraint that enforces a password-complexity policy:
 * at least 8 characters, one uppercase letter, one digit, and one special
 * character (@#$%^&+=!), with no whitespace. Applied at the field level on the
 * {@code password} component of {@code RegisterRequest}.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must be at least 8 characters and contain at least one uppercase letter, one digit, and one special character (@#$%^&+=!)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
