package com.edgareldy.springboottutorial.dto.auth;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.validation.password.PasswordsMatch;
import com.edgareldy.springboottutorial.validation.password.ValidPassword;
import com.edgareldy.springboottutorial.validation.unique.Unique;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload accepted by {@code POST /api/v1/auth/register}.
 * Annotated with {@link ValidPassword} (complexity rules), {@link PasswordsMatch}
 * (confirmation field), and {@link Unique} (username and email uniqueness checked
 * via JPQL before the service transaction opens) to demonstrate layered Bean
 * Validation - field-level, cross-field, and database-backed constraints.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@PasswordsMatch
public record RegisterRequest(

        @NotBlank(message = "First name must not be blank")
        @JsonProperty("first_name") String firstName,

        @NotBlank(message = "Last name must not be blank")
        @JsonProperty("last_name") String lastName,

        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Unique(entityClass = AppUser.class, fieldName = "username",
                message = "Username is already in use")
        String username,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        @Unique(entityClass = AppUser.class, fieldName = "email", caseSensitive = false,
                message = "Email is already in use")
        String email,

        @NotBlank(message = "Password must not be blank")
        @ValidPassword
        String password,

        @NotBlank(message = "Password confirmation must not be blank")
        @JsonProperty("confirm_password") String confirmPassword
) {

    // Overrides the compiler-generated toString(): LoggingAspect logs every
    // service method argument via toString(), and register() takes this
    // record directly, so the default record toString() would put the raw
    // passwords in the application logs.
    @Override
    public String toString() {
        return "RegisterRequest[firstName=%s, lastName=%s, username=%s, email=%s, password=***, confirmPassword=***]"
                .formatted(firstName, lastName, username, email);
    }
}
